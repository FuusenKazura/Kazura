import chisel3.iotesters
import chisel3.iotesters._
import kazura.{modules => M}
import kazura.{stages => S}
import modules.FetchUnitTester
import stages.{BusyBitUnitTest, RFUnitTester}

class Tester extends ChiselFlatSpec {
  // private val backendNames = Array("firrtl", "verilator")
  private val backendNames = Array("firrtl")
  for ( backendName <- backendNames ) {
    behavior of "modules"
    "Fetch" should s"unit test (with $backendName)" in {
      Driver(() => new M.Fetch, backendName) {
        c: M.Fetch => new FetchUnitTester(c)
      } should be (true)
    }
    behavior of "stages"
    "ID" should s"stall function unit test (with $backendName)" in {
      Driver(() => new S.ID, backendName) {
        c: S.ID => new BusyBitUnitTest(c)
      } should be (true)
    }
    "ID" should s"RF function unit test (with $backendName)" in {
      Driver(() => new S.ID, backendName) {
        c: S.ID => new RFUnitTester(c)
      } should be (true)
    }
  }
}
