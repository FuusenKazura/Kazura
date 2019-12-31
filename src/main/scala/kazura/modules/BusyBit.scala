package kazura.modules

import chisel3._
import kazura.util.Params._

// 要求しているレジスタが使用中か確認: WAW, RAW, WARを防ぐ
// Using: Scoreboard algorithm, see: コンピュータアーキテクチャ定量的アプローチ 付録C
class BusyBitIO extends Bundle {
  val release: Vec[RFWrite] = Vec(RF.WRITE_PORT, Input(new RFWrite))
  val req_rs_addr: Vec[UInt] = Vec(RF.READ_PORT, Input(UInt(RF.NUM_W.W)))
  val req_rd_w: Bool = Input(Bool())
  val req_rd_addr: UInt = Input(UInt(RF.NUM_W.W))

  val rs_available: Vec[Bool] = Vec(RF.READ_PORT, Output(Bool()))
}
class BusyBit extends Module {
  val io: BusyBitIO = IO(new BusyBitIO)
  val busy_bit: Vec[Bool] = RegInit(VecInit(Seq.fill(RF.NUM)(false.B)))

  for(i <- 0 until RF.READ_PORT) {
    io.rs_available(i) := (!busy_bit(io.req_rs_addr(i))).||(
      io.release(0).rf_w && io.req_rs_addr(i) === io.release(0).rd_addr) // forwarding
  }

  for (i <- 0 until RF.WRITE_PORT; l <- 0 until RF.NUM) {
    // releaseとreserveが同じbusy_bitに起こることは無いので安全
    when (io.release(i).rf_w && io.release(i).rd_addr === l.U) {
      busy_bit(io.release(i).rd_addr) := false.B
    } .elsewhen (io.req_rd_w && io.req_rd_addr === l.U) {
      busy_bit(io.req_rd_addr) := true.B
    }
  }
  printf("req_rd_w: %d, req_rd_addr: %d\n", io.req_rd_w, io.req_rd_addr)
  printf("busy_bit: "); for (i <- 0 until RF.NUM) printf("%d, ", busy_bit(i)); printf("\n")

  // $0レジスタは常に書き込み可能
  busy_bit(0) := false.B
}

