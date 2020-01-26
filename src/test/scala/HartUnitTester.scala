import chisel3.UInt
import chisel3.iotesters._
import kazura._
import kazura.util.Params._
import chisel3.fromStringToLiteral

object SimpleUnitTest {
  val addUnitTest: Seq[UInt] = Seq(
    "b0000_000_000_00_0000".U,
    "b1001_001_000000101".U,   // $1 = 5
    "b1001_010_000000010".U,   // $2 = 2
    "b0001_001_010_00_0000".U, // $1 = $1 + $2
  ) ++ Seq.fill(100)("h0000".U)

  val jumpUnitTest: Seq[UInt] = Seq(
    "b1001_001_000000000".U, // 0: $1 = 0
    "b1110_000000000010".U,  // 1: pc += 2
    "b0101_001_000000010".U, // 2: $1 += 2
    "b0101_001_000000001".U, // 3: $1 += 1
  ) ++ Seq.fill(100)("h0000".U) // ans: $1 == 1

  val jumpUnitTest2: Seq[UInt] = Seq(
    "b1001_001_000000001".U, // 0: $1 = 1
    "b1110_000_11111_1101".U, // 1: pc -= 1
    "b0101_001_000000001".U, // 2: $1 += 1
  ) ++ Seq.fill(100)("h0000".U) // ans: $1 == 1

  val beqUnitTest: Seq[UInt] = Seq(
    "b1001_001_000000001".U,  // 0: $1 = 1
    "b1001_010_000000001".U,  // 1: $2 = 1
    "b1100_001_010_000010".U, // 2: if($1==$2) pc += 2
    "b0101_001_000000001".U,  // 3: $1 += 1
  ) ++ Seq.fill(100)("h0000".U) // ans: $1 == 1

  val bgtUnitTest: Seq[UInt] = Seq(
    "b1001_001_000000001".U,  // 0: $1 = 1
    "b1001_010_000000001".U,  // 1: $2 = 1
    "b1101_001_010_000010".U, // 2: if($1>$2) pc += 2
    "b0101_001_000000001".U,  // 3: $1 += 1
  ) ++ Seq.fill(100)("h0000".U) // ans: $1 == 2

  // (0 to 10).sum
  val sumBeqUnitTest: Seq[UInt] = Seq(
    "b1001_001_000000000".U,  // 0: $1 = 0
    "b1001_010_000000000".U,  // 1: $2 = 0
    "b1001_011_000001010".U,  // 2: $3 = 10
    "b1001_100_000000000".U,  // 3: $4 = 0
    "b0101_001_000000001".U,  // 4: $1 += 1
    "b0001_010_001_000000".U, // 5: $2 += $1
    "b1100_001_011_000011".U, // 6: if($1 == $3) pc+= 3
    "h0000".U,                // 7: NOP
    "b1110_000_111111100".U,  // 8: pc = pc - 3 - 1
    "b0001_100_010_000000".U  // 9: $4 += $2
  ) ++ Seq.fill(100)("h0000".U) // ans: $4 == (0 to 10).sum

  // (0 to 10).sum
  val sumBgtUnitTest: Seq[UInt] = Seq(
    "b1001_001_000000000".U,  // 0: $1 = 0
    "b1001_010_000000000".U,  // 1: $2 = 0
    "b1001_011_000001001".U,  // 2: $3 = 9
    "b1001_100_000000000".U,  // 3: $4 = 0
    "b0101_001_000000001".U,  // 4: $1 += 1
    "b0001_010_001_000000".U, // 5: $2 += $1
    "b1101_001_011_000011".U, // 6: if($1 > $3) pc+= 3
    "h0000".U,                // 7: NOP
    "b1110_000_111111100".U,  // 7: pc = pc - 3 - 1
    "b0001_100_010_000000".U  // 8: $4 += $2
  ) ++ Seq.fill(100)("h0000".U) // ans: $4 == (0 to 10).sum
}

trait HasInstructions {
  val instructions: Seq[UInt]
}

class SimpleAddUnitTester(m: Hart) extends PeekPokeTester(m) {
  step(20)
  expect(m.io.rf(1), 7)
  expect(m.io.rf(2), 2)
}

class SimpleJumpUnitTester(m: Hart) extends PeekPokeTester(m) {
  step(20)
  expect(m.io.rf(1), 1)
}

class SimpleBeqUnitTester(m: Hart) extends PeekPokeTester(m) {
  step(20)
  expect(m.io.rf(1), 1)
}

object SimpleMemUnitTest extends HasInstructions {
  val instructions: Seq[UInt] = Seq(
    "b1001_001_000_000001".U,  // $1 <- 1
    "b1011_001_000_000000".U, // Mem[$0+0] <- $1
    "b1010_010_000_000000".U, // $2 <- Mem[$0+0]
    "b1001_011_000001010".U,  // $3 <- 10
    "b1001_100_000001011".U,  // $4 <- 11
    "b1001_101_000001100".U,  // $5 <- 12
    "b1001_110_000001101".U,  // $6 <- 13
    "b1011_001_000_000001".U, // Mem[$0+1] <- $3
    "b1011_001_000_000010".U, // Mem[$0+2] <- $4
    "b1011_001_000_000011".U, // Mem[$0+3] <- $5
    "b1011_001_000_000100".U, // Mem[$0+4] <- $6
    "b1010_011_001_000000".U, // $3 <- Mem[$1+0]
    "b1010_100_001_000001".U, // $4 <- Mem[$1+1]
    "b1010_101_001_000010".U, // $5 <- Mem[$1+2]
  ) ++ Seq.fill(32)("h0000".U)
  class Tester(m: Hart) extends PeekPokeTester(m) {
    step(8)
    expect(m.io.rf(1), 1)
    expect(m.io.rf(2), 1)
    // expect(m.io.rf(3), 11)
    // expect(m.io.rf(4), 12)
    // expect(m.io.rf(5), 13)
  }
}

class SimpleBgtUnitTester(m: Hart) extends PeekPokeTester(m) {
  step(10)
  expect(m.io.rf(1), 2)
}

class SumUnitTester(m: Hart) extends PeekPokeTester(m) {
  step(200)
  expect(m.io.rf(4), (0 to 10).sum)
}

object Problem1 extends HasInstructions {
  val instructions: Seq[UInt] = Seq(
    "b1001_001_0_1111_1010".U, // 0: $1 = 250
    "b1001_010_0_0000_0000".U, // 1: $2 = 0
    "b1001_011_0_0000_0001".U, // 2: $3 = 1
    "b0001_010_011_00_0000".U, // 3: $2 += $3
    "b0101_011_000_00_0001".U, // 4: $3 += 1
    "b1101_001_010_11_1110".U, // 5: if($1 > $2) pc -= 2
    "b0110_011_000_00_0001".U, // 6: r3 -= 1
    "b0010_010_011_00_0000".U, // 7: r2 -= $3
  ) ++ Seq.fill(100)("h0000".U)
  class Tester(m: Hart) extends PeekPokeTester(m) {
    step(200)
    expect(m.io.rf(2), 231)
    // expect(m.io.rf(3), 11)
    // expect(m.io.rf(4), 12)
    // expect(m.io.rf(5), 13)
  }
}
