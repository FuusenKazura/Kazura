package kazura.modules

import chisel3._
import kazura.util.Params._
import kazura.models.InstBits

class RFIO extends Bundle {
  val read_addr: Vec[UInt] = Vec(RF.READ_PORT, Input(UInt(RF.NUM_W.W)))
  val write: Vec[RFWrite] = Vec(RF.WRITE_PORT, Input(new RFWrite))
  val out: Vec[UInt] = Vec(RF.READ_PORT, Output(UInt(LEN.W)))
}

class RFWrite extends Bundle {
  val rd_addr: UInt = UInt(RF.NUM_W.W)
  val rf_w: Bool = Bool()
  val data: UInt = UInt(LEN.W)
}

class RegisterFile extends Module {
  val io: RFIO = IO(new RFIO)
  val rf: Vec[UInt] = Reg(Vec(RF.NUM, UInt(LEN.W)))

  for (l <- 0 until RF.READ_PORT) {
    io.out(l) := rf(io.read_addr(l))
  }

  // chiselはマルチポートの書き込みに対応しているのか？
  for (i <- 0 until RF.WRITE_PORT) {
    when(io.write(i).rf_w) {
      rf(io.write(i).rd_addr) := io.write(i).data
    }
  }
  rf(0) := 0.U
}