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
    "b1100_001_011_000010".U, // 6: if($1 == $3) pc+= 2
    "h0000".U,
    "h0000".U,
    "h0000".U,
    "h0000".U,
    "h0000".U,
    "h0000".U,
    "b1110_000_111110111".U,  // 7: pc = pc - 3 (- 6)
    "b0001_100_010_000000".U  // 8: $4 += $2
  ) ++ Seq.fill(100)("h0000".U) // ans: $4 == (0 until 10).sum

  // (0 to 10).sum
  val sumBgtUnitTest: Seq[UInt] = Seq(
    "b1001_001_000000000".U,  // 0: $1 = 0
    "b1001_010_000000000".U,  // 1: $2 = 0
    "b1001_011_000001001".U,  // 2: $3 = 9
    "b1001_100_000000000".U,  // 3: $4 = 0
    "b0101_001_000000001".U,  // 4: $1 += 1
    "b0001_010_001_000000".U, // 5: $2 += $1
    "b1101_001_011_000010".U, // 6: if($1 > $3) pc+= 2
    "b1110_000_111111101".U,  // 7: pc = pc - 3
    "b0001_100_010_000000".U  // 8: $4 += $2
  ) ++ Seq.fill(100)("h0000".U) // ans: $4 == (0 until 10).sum
}

class SimpleAddUnitTester(m: Hart) extends PeekPokeTester(m) {
  step(10)
  expect(m.io.rf(1), 7)
  expect(m.io.rf(2), 2)
}

class SimpleJumpUnitTester(m: Hart) extends PeekPokeTester(m) {
  step(10)
  expect(m.io.rf(1), 1)
}

class SimpleBeqUnitTester(m: Hart) extends PeekPokeTester(m) {
  step(10)
  expect(m.io.rf(1), 1)
}

class SimpleBgtUnitTester(m: Hart) extends PeekPokeTester(m) {
  step(10)
  expect(m.io.rf(1), 2)
}

class SumUnitTester(m: Hart) extends PeekPokeTester(m) {
  step(1000)
  expect(m.io.rf(4), (0 until 10).sum)
}


