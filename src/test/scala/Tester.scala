import chisel3.{fromIntToLiteral, fromStringToLiteral, fromIntToWidth, UInt}
import chisel3.iotesters._
import kazura.{Hart, modules => M, stages => S}
import kazura.util.Params._
import modules.FetchUnitTester
import stages.{BusyBitUnitTest, RFUnitTester}

class Tester extends ChiselFlatSpec {
  // private val backendNames = Array("firrtl", "verilator")
  // private val backendNames = Array("firrtl")
  private val backendNames = Array("verilator")

  for ( backendName <- backendNames ) {
    // behavior of "modules"
    // "Fetch" should s"unit test (with $backendName)" in {
    //   Driver(() => new M.Fetch, backendName) {
    //     c: M.Fetch => new FetchUnitTester(c)
    //   } should be (true)
    // }
    // behavior of "stages"
    // "ID" should s"stall function unit test (with $backendName)" in {
    //   Driver(() => new S.ID, backendName) {
    //     c: S.ID => new BusyBitUnitTest(c)
    //   } should be (true)
    // }
    // "ID" should s"RF function unit test (with $backendName)" in {
    //   Driver(() => new S.ID, backendName) {
    //     c: S.ID => new RFUnitTester(c)
    //   } should be (true)
    // }
    // behavior of "Hart"
    // "Hart" should s"Simple Add unit test (with $backendName)" in {
    //   Driver(() => new Hart(SimpleUnitTest.addUnitTest), backendName) {
    //     c: Hart => new SimpleAddUnitTester(c)
    //   } should be (true)
    // }
    // "Hart" should s"Simple Jump unit test (with $backendName)" in {
    //   Driver(() => new Hart(SimpleUnitTest.jumpUnitTest), backendName) {
    //     c: Hart => new SimpleJumpUnitTester(c)
    //   } should be (true)
    // }
    // "Hart" should s"Simple Jump2 unit test (with $backendName)" in {
    //   Driver(() => new Hart(SimpleUnitTest.jumpUnitTest2), backendName) {
    //     c: Hart => new SimpleJumpUnitTester(c)
    //   } should be (true)
    // }
    // "Hart" should s"Simple beq unit test (with $backendName)" in {
    //   Driver(() => new Hart(SimpleUnitTest.beqUnitTest), backendName) {
    //     c: Hart => new SimpleBeqUnitTester(c)
    //   } should be (true)
    // }
    // "Hart" should s"Simple bgt unit test (with $backendName)" in {
    //   Driver(() => new Hart(SimpleUnitTest.bgtUnitTest), backendName) {
    //     c: Hart => new SimpleBgtUnitTester(c)
    //   } should be (true)
    // }
    "Hart" should s"Sum Beq Loop unit test (with $backendName)" in {
      Driver(() => new Hart(SimpleUnitTest.sumBeqUnitTest), backendName) {
        c: Hart => new SumUnitTester(c)
      } should be (true)
    }
    // "Hart" should s"Sum Bgt Loop unit test (with $backendName)" in {
    //   Driver(() => new Hart(SimpleUnitTest.sumBgtUnitTest), backendName) {
    //     c: Hart => new SumUnitTester(c)
    //   } should be (true)
    // }
  }
}
