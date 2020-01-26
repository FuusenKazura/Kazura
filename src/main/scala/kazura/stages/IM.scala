package kazura.stages

import chisel3._
import chisel3.util._
import kazura.models.{Ctrl, InstInfo, WithAddr}
import kazura.modules.RFWrite
import kazura.util.Params._

class IMIO extends Bundle {
  // val valid: Bool = Input(Bool())
  // s_im.io.valid := m_rob.io.commit_inst_info(0).valid && !m_rob.io.commit(0).mispredict
  val inst_info: InstInfo = Input(new InstInfo)
  val rd_out: UInt = Input(UInt(log2Ceil(LEN).W))
  val rob_out: RFWrite = Input(new RFWrite)
  val mem_out: RFWrite  = Output(new RFWrite)
}

class IM extends Module {
  val io: IMIO = IO(new IMIO)
  val mem: Mem[UInt] = Mem(MEM.NUM, UInt(LEN.W))
  val out_data: UInt = Wire(UInt(LEN.W))

  out_data := 0.U
  when (io.inst_info.ctrl.mem_w && !io.rob_out.mispredict) {
    mem.write(io.rob_out.data, io.rd_out)
  } .elsewhen(io.inst_info.ctrl.mem_r) {
    out_data := mem.read(io.rob_out.data)
  }
  io.mem_out := RegNext(io.rob_out)
  io.mem_out.rf_w := RegNext(io.inst_info.ctrl.mem_r)
  io.mem_out.data := RegNext(out_data)

  // printf("MEM WRITE: en: %d, addr: %d, data: %d\n", io.write.valid, io.write.bits.addr, io.write.bits.data)
  // printf("MEM READ: en: %d, addr: %d\n", io.read.valid, io.read.bits)
  // printf("MEM OUT: en: %d, addr: %d, data: %d\n", io.out.valid, io.out.bits.addr, io.out.bits.data)
}
