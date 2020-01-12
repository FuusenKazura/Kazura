package kazura.stages

import chisel3._
import chisel3.util._
import kazura.models.WithAddr
import kazura.util.Params._

class IMIO extends Bundle {
  val rd_addr: UInt = Input(UInt(log2Ceil(RF.NUM).W))
  val read: Valid[UInt] = Input(Valid(UInt(log2Ceil(LEN).W)))
  val write: Valid[WithAddr] = Input(Valid(new WithAddr(log2Ceil(MEM.NUM).W)))
  val out: Valid[WithAddr]  = Output(Valid(new WithAddr(RF.NUM_W.W)))
}

class IM extends Module {
  val io: IMIO = IO(new IMIO)
  val mem: Mem[UInt] = Mem(MEM.NUM, UInt(LEN.W))
  val out_data: UInt = Reg(UInt(LEN.W))

  out_data := 0.U
  when (io.write.valid) {
    mem.write(io.write.bits.addr, io.write.bits.data)
  } .elsewhen(io.read.valid) {
    out_data := mem.read(io.read.bits)
  }

  io.out.valid := RegNext(io.read.valid, false.B)
  io.out.bits.addr := RegNext(io.rd_addr)
  io.out.bits.data := out_data

  // printf("MEM WRITE: en: %d, addr: %d, data: %d\n", io.write.valid, io.write.bits.addr, io.write.bits.data)
  // printf("MEM READ: en: %d, addr: %d\n", io.read.valid, io.read.bits)
  // printf("MEM OUT: en: %d, addr: %d, data: %d\n", io.out.valid, io.out.bits.addr, io.out.bits.data)
}
