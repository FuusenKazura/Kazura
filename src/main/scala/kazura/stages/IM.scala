package kazura.stages

import chisel3._
import chisel3.util._
import kazura.models.{Ctrl, InstInfo, WithAddr}
import kazura.util.Params._

class IMIO extends Bundle {
  val inst_info: InstInfo = Input(new InstInfo)
  val rd_out: UInt = Input(UInt(log2Ceil(LEN).W))
  val alu_out: UInt = Input(UInt(log2Ceil(LEN).W))
  val mem_out: UInt  = Output(UInt(LEN.W))
  val inst_info_out: InstInfo = Output(new InstInfo)
}

class IM extends Module {
  val io: IMIO = IO(new IMIO)
  val mem: Mem[UInt] = Mem(MEM.NUM, UInt(LEN.W))
  val out_data: UInt = Reg(UInt(LEN.W))

  out_data := 0.U
  when (io.inst_info.ctrl.mem_w) {
    mem.write(io.alu_out, io.rd_out)
  } .elsewhen(io.inst_info.ctrl.mem_r) {
    out_data := mem.read(io.alu_out)
  }

  io.mem_out := out_data
  io.inst_info_out := RegNext(io.inst_info, InstInfo.nop)

  // printf("MEM WRITE: en: %d, addr: %d, data: %d\n", io.write.valid, io.write.bits.addr, io.write.bits.data)
  // printf("MEM READ: en: %d, addr: %d\n", io.read.valid, io.read.bits)
  // printf("MEM OUT: en: %d, addr: %d, data: %d\n", io.out.valid, io.out.bits.addr, io.out.bits.data)
}
