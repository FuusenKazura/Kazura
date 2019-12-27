package kazura.modules

import chisel3._
import kazura.util.Params._

// 要求しているレジスタが使用中か確認: WAW, RAW, WARを防ぐ
// Using: Scoreboard algorithm, see: コンピュータアーキテクチャ定量的アプローチ 付録C
class BusyBitIO extends Bundle {
  val release_rd_enable: Bool = Input(Bool())
  val release_rd_addr: UInt = Input(UInt(RF.NUM_W.W))
  val req_rs_addr: Vec[UInt] = Vec(2, Input(UInt(RF.NUM_W.W)))
  val req_rd_addr: UInt = Input(UInt(RF.NUM_W.W))

  val rs_available: Vec[Bool] = Vec(2, Output(Bool()))
  val rd_available: Bool = Output(Bool())
}
class BusyBit extends Module {
  val io: BusyBitIO = IO(new BusyBitIO)
  val busy_bit: Vec[Bool] = RegInit(VecInit(Seq.fill(RF.NUM)(false.B)))

  for(i <- 0 until 2) {
    io.rs_available(i) := !busy_bit(io.req_rs_addr(i))
  }
  io.rd_available := !busy_bit(io.req_rd_addr)
  when (io.release_rd_enable) {
    busy_bit(io.release_rd_addr) := false.B;
  }

  // $0レジスタは常に0
  busy_bit(0) := true.B
}

