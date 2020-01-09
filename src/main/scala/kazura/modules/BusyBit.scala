package kazura.modules

import chisel3._
import chisel3.util._
import kazura.util.Params._

// 要求しているレジスタが使用中か確認: WAW, RAW, WARを防ぐ
// Using: Scoreboard algorithm, see: コンピュータアーキテクチャ定量的アプローチ 付録C
// RegFileと統合したほうがバグりにくい気がする……
class BusyBitIO extends Bundle {
  val branch_mispredicted: Bool = Input(Bool()) // 分岐予測の予測を失敗したか
  val branch_graduated: Bool = Input(Bool()) // 分岐命令の演算が完了したか(完了した演算が分岐であるか)
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
    // forwarding
    val look_forward = io.release.exists((r: RFWrite) => r.rf_w && r.rd_addr === io.req_rs_addr(i)) // forwarding
    io.rs_available(i) := !busy_bit(io.req_rs_addr(i)) || look_forward
  }

  for (i <- 0 until RF.WRITE_PORT; l <- 0 until RF.NUM) {
    when(io.branch_graduated && io.branch_mispredicted) { // 分岐予測に失敗すると状態をリセット
      busy_bit(io.release(i).rd_addr) := false.B
    } .elsewhen (io.release(i).rf_w && io.release(i).rd_addr === l.U) { // releaseとreserveが同じbusy_bitに起こることは無いので安全
      busy_bit(io.release(i).rd_addr) := false.B
    } .elsewhen (io.req_rd_w && io.req_rd_addr === l.U) {
      busy_bit(io.req_rd_addr) := true.B
    }
  }
  // printf("req_rd_w: %d, req_rd_addr: %d\n", io.req_rd_w, io.req_rd_addr)
  printf("busy_bit: "); for (i <- 0 until RF.NUM) printf("%d, ", busy_bit(i)); printf("\n")

  // $0レジスタは常に書き込み可能
  busy_bit(0) := false.B
}

