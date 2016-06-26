package squad.inject

import org.jetbrains.spek.api.Spek
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith


// testing deep graphs with abstract dependencies
interface Parent {
    fun hello(name: String): String
}
interface Child {
    fun uberHello(name: String): String
}

class ParentImpl : Parent {
    override fun hello(name: String): String = "Hello, $name"
}

class ChildImpl(val parent: Parent) : Child {
    override fun uberHello(name: String): String = "${parent.hello(name)}!"
}

class CustomerCode(val child: Child) {
    fun start(): String = child.uberHello("world") + child.uberHello("test")
}

object CustomerModule : Module {
    override fun configure() = listOf(
            bind<Parent, ParentImpl>(),
            bind<Child, ChildImpl>()
    )
}


// testing the number of instances created
interface Counter

class CounterImpl : Counter {
    companion object {
        var numberOfInstances = 0
    }

    init {
        numberOfInstances += 1
    }
}

class CounterDepending(counter: Counter)

object CounterModule : Module {
    override fun configure() = listOf(
            bind<Counter, CounterImpl>()
    )
}


//testing circular dependencies
interface BadCircularDependencyInterface
class BadCircularDependencyClass1(dependency: BadCircularDependencyInterface) : BadCircularDependencyInterface
class BadCircularDependencyClass2(dependency: BadCircularDependencyClass3)
class BadCircularDependencyClass3(dependency: BadCircularDependencyClass2)


//selection
interface Service
interface AmazingService
class ServiceA : Service
class ServiceB : Service
class ServiceC : Service, AmazingService


// actual spec
class SquadInjectTest : Spek({
    describe("SquadInject") {
        beforeEach {
            CounterImpl.numberOfInstances = 0
        }

        it("should instantiate a valid graph") {
            val graph = Graph(CustomerModule)
            val code = graph.get<CustomerCode>()

            assertEquals("Hello, world!Hello, test!", code.start())
        }

        it("should instantiate only once by graph") {
            val graph = Graph(CounterModule)

            graph.get<Counter>()
            graph.get<Counter>()
            graph.get<CounterDepending>()

            assertEquals(1, CounterImpl.numberOfInstances)
        }

        it("should support scopes") {
            val graph = Graph(CounterModule)
            val scope1 = graph.scope()
            val scope2 = graph.scope()
            val scope3 = graph.scope()

            scope1.get<Counter>()        //+1, parent has no counter, we have to create one in this scope
            scope2.get<Counter>()        //+1, same
            graph.get<Counter>()         //+1, same, parent now have a counter
            scope3.get<Counter>()        //+1, parent had no counter at the time we scoped
            graph.scope().get<Counter>() //+0, parent do have a counter now

            assertEquals(4, CounterImpl.numberOfInstances)
        }

        it("should throw an exception if no binding has been configured") {
            val graph = Graph()

            assertFailsWith<IllegalArgumentException>() {
                graph.get<Parent>()
            }
            assertFailsWith<IllegalArgumentException>() {
                graph.get<ChildImpl>()
            }
        }

        xit("should detect circular dependencies") {
            val graph = Graph(object : Module {
                override fun configure() = listOf(bind<BadCircularDependencyInterface, BadCircularDependencyClass1>())
            })

            assertFailsWith<IllegalArgumentException>("inheritance") {
                graph.get<BadCircularDependencyClass1>()
            }
            assertFailsWith<IllegalArgumentException>("dumb circularity") {
                graph.get<BadCircularDependencyClass2>()
            }
        }

        it("select multiple instances") {
            val graph = Graph(CustomerModule, object : Module {
                override fun configure() = listOf(
                        declare<ServiceA>(),
                        declare<ServiceB>(),
                        bind<AmazingService, ServiceC>()
                )
            })

            val services = graph.select(Service::class.java)
            assertEquals(3, services.size)
        }
    }
})