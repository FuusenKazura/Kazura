package kazura.modules

import chisel3._
import chisel3.util._
import kazura.util.Params._

class BPLearning extends Bundle {
  val pc: UInt = UInt(LEN.W)
  val result: Bool = Bool()
}

class BPIO extends Bundle {
  val pc: UInt = Input(UInt(LEN.W))
  val learning: Valid[BPLearning] = Input(Valid(new BPLearning))
  val predict: Bool = Output(Bool())
}

class CutoffHeadBits(width: Int) extends Bundle {
  val head: UInt = UInt((LEN - width).W)
  val tail: UInt = UInt(width.W)
}

// 飽和カウンタ
class BranchPredictor(val size: Int = 32) extends Module {
  val io = IO(new BPIO)
  val table: Mem[UInt] = Mem.apply(size, UInt(2.W))

  def increment(x: UInt): UInt = MuxLookup(x, "b00".U, Seq(
    "b00".U -> "b01".U,
    "b01".U -> "b10".U,
    "b10".U -> "b11".U,
    "b11".U -> "b11".U
  ))
  def decrement(x: UInt): UInt = MuxLookup(x, "b00".U, Seq(
    "b00".U -> "b00".U,
    "b01".U -> "b00".U,
    "b10".U -> "b01".U,
    "b11".U -> "b10".U
  ))
  def update(f: Bool, x: UInt): UInt = Mux(f, increment(x), decrement(x))
  def judge(x: UInt): Bool = MuxLookup(x, true.B, Seq(
    "b00".U -> false.B,
    "b01".U -> false.B,
    "b10".U -> true.B,
    "b11".U -> true.B
  ))

  // 学習には2クロックの遅延が必要
  // 連続して同じアドレスを更新することは考えづらいので大丈夫
  val addr: UInt = RegNext(io.learning.bits.pc)
  val updated: UInt = RegNext(update(io.learning.bits.result, table.read(io.learning.bits.pc)))
  when (RegNext(io.learning.valid, false.B)) {
    table.write(addr, updated)
  }

  io.predict := judge(table.read(io.pc))
}
