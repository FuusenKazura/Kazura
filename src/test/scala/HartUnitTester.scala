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
    "b1001_001_000_000111".U,  // $1 <- 7
    "b1011_001_000_000000".U, // Mem[$0+0] <- $1
    "b1010_010_000_000000".U, // $2 <- Mem[$0+0]
    "b1001_011_000001010".U,  // $3 <- 10
    "b1001_100_000001011".U,  // $4 <- 11
    "b1001_101_000001100".U,  // $5 <- 12
    "b1001_110_000001101".U,  // $6 <- 13
    "b1011_011_000_000111".U, // Mem[$0+7] <- $3
    "b1011_100_000_001000".U, // Mem[$0+8] <- $4
    "b1011_101_000_001001".U, // Mem[$0+9] <- $5
    "b1011_110_000_001010".U, // Mem[$0+10] <- $6
    "b1010_011_001_000001".U, // $3 <- Mem[$1+1]
    "b1010_100_001_000010".U, // $4 <- Mem[$1+2]
    "b1010_101_001_000011".U, // $5 <- Mem[$1+3]
  ) ++ Seq.fill(100)("h0000".U)
  class Tester(m: Hart) extends PeekPokeTester(m) {
    step(100)
    expect(m.io.rf(1), 7)
    expect(m.io.rf(2), 7)
    expect(m.io.rf(3), 11)
    expect(m.io.rf(4), 12)
    expect(m.io.rf(5), 13)
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
    "b1010_001_0_0000_0000".U, // 0: $1 = Mem[0]
    "b1001_010_0_0000_0000".U, // 1: $2 = 0
    "b1001_011_0_0000_0001".U, // 2: $3 = 1
    "b0001_010_011_00_0000".U, // 3: $2 += $3
    "b0101_011_000_00_0001".U, // 4: $3 += 1
    "b1101_001_010_11_1110".U, // 5: if($1 > $2) pc -= 2
    "b0110_011_000_00_0001".U, // 6: r3 -= 1
    "b0010_010_011_00_0000".U, // 7: r2 -= $3
    "hffff".U,                 // 8: halt
  ) ++ Seq.fill(500)("h0000".U)
  class Tester(m: Hart) extends PeekPokeTester(m) {
    step(2000)
    expect(m.io.rf(2), 29890)
  }
}

object Problem2A extends HasInstructions {
  val instructions: Seq[UInt] = Seq(
    "b1010_010_000_00_0011".U, //  0: $2 = Mem[3] - X
  // LOG2:
    "b1001_111_000_00_0001".U, //  1: $7 = 1
    "b1001_011_000_00_0010".U, //  2: $3 = 2
    "b1001_100_000_00_0010".U, //  3: $4 = 2
  // LOG2_OUTER:
    "b1001_101_000_00_0000".U, //  4: $5 = 0
    "b1001_110_000_00_0000".U, //  5: $6 = 0
  // MUL_OUTER:
    "b0001_101_011_00_0000".U, //  6: $5 += $3
    "b0101_110_000_00_0001".U, //  7: $6 += 1
    "b1101_100_110_11_1110".U, //  8: if ($4 > $6) goto MUL_OUTER;
    "b1001_011_000_00_0000".U, //  9: $3 = 0
    "b0001_011_101_00_0000".U, // 10: $3 += $5
    "b0101_111_000_00_0001".U, // 11: $7 += 1
    "b1100_101_010_00_0010".U, // 12: if (r[5] == r[2]) goto CREATE_MASK;
    "b1110_000_111_11_0111".U, // 13: goto LOG2_OUTER; (-9)
  // CREATE_MASK:
    "b1001_010_000_00_0000".U, // 14: $2 = 0;
    "b1001_011_000_00_0000".U, // 15: $3 = 0;
    "b1001_100_000_00_0010".U, // 16: $4 = 2;
    "b1001_101_000_00_0000".U, // 17: $5 = 0;
  // MASK_OUTER:
    "b1001_011_000_00_0001".U, // 18: $3 = 1;
    "b0001_011_101_00_0000".U, // 19: $3 += $5
    "b1001_101_000_00_0000".U, // 20: $5 = 0;
    "b1001_110_000_00_0000".U, // 21: $6 = 0;
  // MUL2_OUTER:
    "b0001_101_011_00_0000".U, // 22: $5 += $3;
    "b0101_110_000_00_0001".U, // 23: $6 += 1;
    "b1101_100_110_11_1110".U, // 24: if (r[4] > r[6]) goto MUL2_OUTER;(-2)
    "b0110_111_000_00_0001".U, // 25: $7 -= 1;
    "b1101_111_000_11_1000".U, // 26: if (r[7] > r[0]) goto MASK_OUTER;(-8)
    "b1010_001_000_00_0001".U, // 27: $1 = Mem[1] - a
    "b1010_010_000_00_0010".U, // 28: $2 = Mem[2] - b
    "b1001_101_000_00_0000".U, // 29: $5 = 0
  // MAIN_OUTER:
    "b1001_100_000_00_0000".U, // 30: $4 = 0
    "b0001_100_001_00_0000".U, // 31: $4 += $1
    "b0011_100_011_00_0000".U, // 32: $4 &= $3
    "b1101_100_000_00_0010".U, // 33: if (r[4] > r[0]) goto MAIN_SKIP;
    "b0101_101_000_00_0001".U, // 34: $5 += 1
  // MAIN_SKIP:
    "b0101_001_000_00_0001".U, // 35: $1 += 1
    "b1101_010_001_11_1010".U, // 36: if (r[2] > r[1]) goto MAIN_OUTER;
    "h0000".U,                 //
    "h0000".U,                 //
    "h0000".U,                 //
    "h0000".U,                 //
    "h0000".U,                 //
    "h0000".U,                 //
    "h0000".U,                 //
    "hffff".U                  // 37: halt
  ) ++ Seq.fill(500)("h0000".U)
  class Tester(m: Hart) extends PeekPokeTester(m) {
    step(3000)
    expect(m.io.rf(5), 12)
  }
}


object Problem3B extends HasInstructions {
  val instructions: Seq[UInt] = Seq(
  "b1001_001_000_00_0111".U, //  0: int i_1=7;
  "b1001_110_000_10_0111".U, //  1: int upper_6=39;
  // OUTER:
  "b1001_010_000_00_0000".U, //  2: min_index_2 = 0
  "b0001_010_001_00_0000".U, //  3: min_index_2 += i_1;
  "b1010_011_010_00_0000".U, //  4: min_3 = mem[min_index_2];
  "b1001_101_000_00_0000".U, //  5: l_5 = 0;
  "b0001_101_001_00_0000".U, //  6: l_5 += i_1;
  // INNER:
  "b1010_100_101_00_0000".U, //  7: tmp_4 = mem[l_5];
  "b1101_100_011_00_0100".U, //  8: if (tmp_4 > min_3) goto SKIP;
  "b1001_010_000_00_0000".U, //  9: min_index_2 = 0;
  "b0001_010_101_00_0000".U, // 10: min_index_2 += l_5;
  "b1010_011_101_00_0000".U, // 11: min_3 = mem[l_5];
  // SKIP:
  "b0101_101_000_00_0001".U, // 12: l_5 += 1;
  "b1101_110_101_11_1010".U, // 13: if(upper_6>l_5) goto INNER;
  "b1010_100_001_00_0000".U, // 14: tmp_4 = mem[i_1];
  "b1011_011_001_00_0000".U, // 15: mem[i_1] = min_3;
  "b1011_100_010_00_0000".U, // 16: mem[min_index_2] = tmp_4;
  "b0101_001_000_00_0001".U, // 17: i_1 += 1;
  "b1101_110_001_11_0000".U, // 18: if (upper_6>i_1) goto OUTER;
  "h0000".U,
  "b1010_001_000_00_0111".U,
  "b1010_010_000_00_1000".U,
  "b1010_011_000_00_1001".U,
  "b1010_100_000_00_1010".U,
  "b1010_101_000_00_1011".U,
  "b1010_110_000_00_1100".U,
  "b1010_111_000_00_1101".U,
  "h0000".U,
  "h0000".U,
  "h0000".U,
  "h0000".U,
  "h0000".U,
  "hffff".U,
  ) ++ Seq.fill(500)("h0000".U)
  class Tester(m: Hart) extends PeekPokeTester(m) {
    step(10000)
    expect(m.io.rf(1), 2)
    expect(m.io.rf(2), 3)
    expect(m.io.rf(3), 4)
    expect(m.io.rf(4), 14)
    expect(m.io.rf(5), 22)
    expect(m.io.rf(6), 28)
    expect(m.io.rf(7), 29)
  }
}
