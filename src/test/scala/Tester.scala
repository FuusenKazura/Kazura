import chisel3.iotesters
import chisel3.iotesters._
import kazura.modules.Fetch
import modules.FetchUnitTester

class Tester extends ChiselFlatSpec {
  // private val backendNames = Array("firrtl", "verilator")
  private val backendNames = Array("firrtl")
  for ( backendName <- backendNames ) {
    behavior of "modules"
    "Fetch" should s"unit test (with $backendName)" in {
      Driver(() => new Fetch, backendName) {
        c: Fetch => new FetchUnitTester(c)
      } should be (true)
    }
  }
}
