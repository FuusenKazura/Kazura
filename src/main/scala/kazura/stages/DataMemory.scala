package kazura.stages

import chisel3._
import kazura.models.InstInfo
import kazura.modules.{Memory, RFWrite}
import kazura.util.Params._

class DataMemoryIO extends Bundle {
  val inst_info: InstInfo = Input(new InstInfo)
  val rd_out: UInt = Input(UInt(LEN.W))
  val rob_out: RFWrite = Input(new RFWrite)
  val mem_out: RFWrite  = Output(new RFWrite)
}

class DataMemory(val init_data: Seq[UInt]) extends Module {
  val io: DataMemoryIO = IO(new DataMemoryIO)
  val mem: Memory = Module(new Memory(init_data))

  val out_data: UInt = Wire(UInt(LEN.W))

  out_data := 0.U
  mem.io.mem_w := io.inst_info.ctrl.mem_w && !io.rob_out.mispredict
  mem.io.mem_r := io.inst_info.ctrl.mem_r
  mem.io.address := io.rob_out.data
  mem.io.in_data := io.rd_out

  io.mem_out := RegNext(io.rob_out)
  io.mem_out.rf_w := RegNext(io.inst_info.ctrl.mem_r && !io.rob_out.mispredict)
  io.mem_out.data := mem.io.out_data
}
