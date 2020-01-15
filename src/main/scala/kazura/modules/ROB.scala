package kazura.modules

import chisel3._
import chisel3.util._
import kazura.util.Params

class ROBIO extends Bundle {
  val reserved: UInt = Input(UInt(log2Ceil(Params.ROB.RESERVE_SIZE).W))
  val available: Vec[Valid[UInt]] = Output(Vec(Params.ROB.RESERVE_SIZE, Valid(UInt(Params.LEN.W))))
}

class ROBIn extends Bundle {

}

class ROBOut extends Bundle {

}

class ROBEntry extends Bundle {
  val empty: Bool = Bool()
}

class ROB extends Module {
  val io: ROBIO = IO(new ROBIO)

  val buf_init: ROBEntry = Wire(new ROBEntry)
  buf_init.empty := true.B

  val buf: Vec[ROBEntry] = RegInit(VecInit(Seq.fill(Params.ROB.BUF_SIZE)(buf_init)))
  val current_head: UInt = RegInit(0.U)
  val reserve_head: UInt = RegInit(0.U)

  // io.available.valid := buf(reserve_head).empty
  // io.available.bits := reserve_head

  for (i <- 0 until Params.ROB.BUF_SIZE) {
    buf(i).empty := false.B
  }
}
