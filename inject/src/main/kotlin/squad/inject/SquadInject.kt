package squad.inject

import org.pcollections.HashTreePMap
import org.pcollections.HashTreePSet
import org.pcollections.PMap
import org.pcollections.PSet

data class Binding(val from: Class<*>, val to: Class<*>)

inline fun <reified T: Any, reified U: T> bind(): Binding = Binding(T::class.java, U::class.java)
inline fun <reified T: Any> declare(): Binding = Binding(T::class.java, T::class.java)

interface Module {
    fun configure(): List<Binding>
}

private fun loadModules(modules: Array<out Module>): PMap<Class<*>, Class<*>> {
    var res = HashTreePMap.empty<Class<*>, Class<*>>()

    for (module in modules) {
        for (binding in module.configure()) {
            res = res.plus(binding.from, binding.to)
        }
    }

    return res
}

class Graph private constructor(private val bindings: PMap<Class<*>, Class<*>>, private var state: PMap<Class<*>, Any> = HashTreePMap.empty()) {

    constructor(vararg modules: Module) : this(loadModules(modules)) {}

    private fun findCompleteInheritance(klass: Class<*>): PSet<Class<*>> {
        var res = HashTreePSet.singleton(klass)

        if (klass.superclass != null && klass.superclass != Any::class.java) {
            res = res.plusAll(findCompleteInheritance(klass.superclass))
        }

        for (iface in klass.interfaces) {
            res = res.plusAll(findCompleteInheritance(iface))
        }

        return res
    }

    fun scope(vararg modules: Module) = Graph(bindings.plusAll(loadModules(modules)), state)

    fun <T: Any> get(klass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        if (state.containsKey(klass)) {
            return state[klass] as T
        }

        val inheritance = findCompleteInheritance(klass)
        val classBindings = inheritance.mapNotNull { bindings[it] }
        val binding = classBindings.firstOrNull() ?: klass
        if (binding.isInterface) {
            throw IllegalArgumentException("abstract dependency `${binding.name}' should be bound in a module")
        }

        val constructor = binding.constructors.firstOrNull()

        val instance =
            @Suppress("UNCHECKED_CAST")
            if (constructor != null) {
                constructor.parameters.forEach {
                    if (inheritance.contains(it.type)) {
                        throw IllegalArgumentException("`${binding.name}' dependency `${it.name}' is circular")
                    }
                }
                val params = constructor.parameters.map{ get(it.type) }.toTypedArray()
                constructor.newInstance(*params) as T
            } else {
                binding.newInstance() as T
            }

        state = state.plus(binding, instance).plus(klass, instance)

        return instance
    }

    fun <T: Any> select(klass: Class<T>): Set<T> {
        var res = HashTreePSet.empty<T>()

        for ((from, to) in bindings) {
            @Suppress("UNCHECKED_CAST")
            if (klass.isAssignableFrom(to)) {
                res = res.plus(get(to) as T)
            }
        }

        return res
    }
}

inline fun <reified T: Any> Graph.get(): T = get(T::class.java)
