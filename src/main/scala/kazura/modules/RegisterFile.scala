package kazura.modules

import chisel3._
import chisel3.util.experimental.BoringUtils
import chisel3.util.log2Ceil
import kazura.util.Params._
import kazura.models.InstBits

class RFIO extends Bundle {
  val read_addr: Vec[UInt] = Vec(RF.READ_PORT, Input(UInt(RF.NUM_W.W)))
  val write: Vec[RFWrite] = Vec(RF.WRITE_PORT, Input(new RFWrite))
  val out: Vec[UInt] = Vec(RF.READ_PORT, Output(UInt(LEN.W)))
  val rf4debug: Vec[UInt] = Vec(RF.NUM, Output(UInt(LEN.W)))
}

class RFWrite extends Bundle {
  val rd_addr: UInt = UInt(RF.NUM_W.W)
  val rob_addr: UInt = UInt(log2Ceil(ROB.BUF_SIZE).W)
  val mispredict: Bool = Bool()
  val rf_w: Bool = Bool()
  val data: UInt = UInt(LEN.W)
}

class RegisterFile extends Module {
  val io: RFIO = IO(new RFIO)
  val rf: Vec[UInt] = Reg(Vec(RF.NUM, UInt(LEN.W)))

  for (l <- 0 until RF.READ_PORT) {
    // forwarding
    // val look_forward: RFWrite = Wire(new RFWrite)
    // look_forward.rf_w := false.B
    // look_forward.rd_addr := 0.U
    // look_forward.data := 0.U
    val look_forward: RFWrite = io.write.reduceTree((r1: RFWrite, r2: RFWrite) =>
      Mux(r1.rf_w && !r1.mispredict && r1.rd_addr === io.read_addr(l), r1, r2))
    io.out(l) := Mux(look_forward.rf_w && !look_forward.mispredict && (look_forward.rd_addr === io.read_addr(l)),
      look_forward.data, rf(io.read_addr(l)))
  }

  // chiselはマルチポートの書き込みに対応しているのか？
  for (i <- 0 until RF.WRITE_PORT) {
    when(io.write(i).rf_w && !io.write(i).mispredict) {
      rf(io.write(i).rd_addr) := io.write(i).data
    }
  }
  rf(0) := 0.U

  // テスト用のデバッグポート, chisel3.experimental.BoreUtilsは挙動が怪しくてダメだった
  io.rf4debug := rf

  // for (i <- 0 until RF.WRITE_PORT) {
  //   printf(s"we: %d, addr: %d, data: %d\n", io.write(i).rf_w, io.write(i).rd_addr, io.write(i).data)
  // }

  // printf(s"rf: ")
  // for (i <- 0 until RF.NUM -1)
  //   printf(s"$i:%d, ", rf(i))
  // printf(s"${RF.NUM -1}:%d\n", rf(RF.NUM -1))
}