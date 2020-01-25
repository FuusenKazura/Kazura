package modules

import chisel3.iotesters._
import kazura.modules.ROB
import kazura.util.Params

// PARALLEL=1
class ROBUnitTester(m: ROB) extends PeekPokeTester(m) {
  assert(Params.PARALLEL == 1, "このテストはPARALLEL=1用")

  poke(m.io.used_num, 0) // 0reserve
  poke(m.io.graduate(0).valid, false)
  poke(m.io.graduate(0).bits.mispredicted, false)
  poke(m.io.graduate(1).valid, false)
  step(1) //------------------------------------------------------------

  expect(m.io.commit(0).rf_w, false)
  expect(m.io.unreserved_head(0).valid, true, "初期値")
  expect(m.io.unreserved_head(0).bits, 0, "初期値")
  poke(m.io.used_num, 1) // 1つreserve
  poke(m.io.graduate(0).valid, false);
  poke(m.io.graduate(0).bits.mispredicted, false)
  poke(m.io.graduate(1).valid, false)
  step(1) //------------------------------------------------------------

  expect(m.io.commit(0).rf_w, false)
  expect(m.io.unreserved_head(0).valid, true, "確保(=> unreserved_headを2に)")
  expect(m.io.unreserved_head(0).bits, 1, "確保(=> unreserved_headを2に)")
  poke(m.io.used_num, 1) // 1つreserve
  poke(m.io.graduate(0).valid, false);
  poke(m.io.graduate(0).bits.mispredicted, false)
  poke(m.io.graduate(1).valid, false)
  step(1) //------------------------------------------------------------

  expect(m.io.commit(0).rf_w, false)
  expect(m.io.unreserved_head(0).valid, true, "確保(=> unreserved_headを3に)")
  expect(m.io.unreserved_head(0).bits, 2, "確保(=> unreserved_headを3に)")
  poke(m.io.used_num, 1) // 1つreserve
  poke(m.io.graduate(0).valid, false);
  poke(m.io.graduate(0).bits.mispredicted, false)
  poke(m.io.graduate(1).valid, false)
  step(1) //------------------------------------------------------------

  expect(m.io.commit(0).rf_w, false)
  expect(m.io.unreserved_head(0).valid, true, "1進める")
  expect(m.io.unreserved_head(0).bits, 3, "1進める")
  poke(m.io.used_num, 0)
  // rob(1)の命令をgraduate, rob(0)がまだなのでcommitは不可能
  poke(m.io.graduate(0).valid, true)
  poke(m.io.graduate(0).bits.mispredicted, false)
  poke(m.io.graduate(0).bits.inst_info.ctrl.rf_w, true)
  poke(m.io.graduate(0).bits.addr, 1)
  poke(m.io.graduate(0).bits.inst_info.rd_addr, 1)
  poke(m.io.graduate(0).bits.data, 10)
  poke(m.io.graduate(1).valid, false)
  step(1) //------------------------------------------------------------

  expect(m.io.commit(0).rf_w, false)
  expect(m.io.unreserved_head(0).valid, true)
  expect(m.io.unreserved_head(0).bits, 3)
  poke(m.io.used_num, 1)
  // rob(0)の命令をgraduate, これでrob(1)以下の命令をcommit可能に
  poke(m.io.graduate(0).valid, true)
  poke(m.io.graduate(0).bits.mispredicted, false)
  poke(m.io.graduate(0).bits.inst_info.ctrl.rf_w, true)
  poke(m.io.graduate(0).bits.addr, 0)
  poke(m.io.graduate(0).bits.inst_info.rd_addr, 2)
  poke(m.io.graduate(0).bits.data, 20)
  poke(m.io.graduate(1).valid, false)
  step(1) //------------------------------------------------------------

  poke(m.io.used_num, 0)
  poke(m.io.graduate(0).valid, true)
  poke(m.io.graduate(0).bits.mispredicted, true)
  poke(m.io.graduate(0).bits.addr, 2)
  poke(m.io.graduate(0).bits.mispredicted, true)
  poke(m.io.graduate(0).bits.mispredicted, true)
  poke(m.io.graduate(1).valid, false)

  expect(m.io.commit(0).rf_w, true, "commit buf(0)")
  expect(m.io.commit(0).rd_addr, 2, "commit buf(0)")
  expect(m.io.commit(0).data, 20, "commit buf(0)")

  expect(m.io.unreserved_head(0).valid, false, "分岐予測失敗時はrestoreするためにreserveはお休み")
  expect(m.io.unreserved_head(0).bits, 4)

  step(1) // ------------------------------------------------------------

  // TODO: mispredicted時のテストを書く
  poke(m.io.used_num, 0)
  poke(m.io.graduate(0).valid, false)
  poke(m.io.graduate(0).bits.mispredicted, false)
  poke(m.io.graduate(1).valid, false)

  expect(m.io.commit(0).rf_w, true, "commit buf(1)")
  expect(m.io.commit(0).data, 10, "commit buf(1)")
  expect(m.io.commit(0).rd_addr, 1, "commit buf(1)")

  expect(m.io.unreserved_head(0).valid, true, "分岐予測失敗後のクロック")
  expect(m.io.unreserved_head(0).bits, 2, "分岐予測失敗後のクロック")
  step(1) // ------------------------------------------------------------
}

// PARALLEL=4
/*
class ROBUnitTester(m: ROB) extends PeekPokeTester(m) {
  poke(m.io.used_num, 0) // 0reserve
  for (i <- m.io.graduate.indices) {
    poke(m.io.graduate(i).valid, false); poke(m.io.graduate(i).bits.mispredicted, false)
  }
  step(1) //------------------------------------------------------------

  for (i <- m.io.commit.indices) expect(m.io.commit(i).rf_w, false)
  for (i <- m.io.unreserved_head.indices) {
    expect(m.io.unreserved_head(i).valid, true, "初期値")
    expect(m.io.unreserved_head(i).bits, 0+i, "初期値")
  }
  poke(m.io.used_num, 4) // ４つreserve
  for (i <- m.io.graduate.indices) {
    poke(m.io.graduate(i).valid, false); poke(m.io.graduate(i).bits.mispredicted, false)
  }
  step(1) //------------------------------------------------------------

  for (i <- m.io.commit.indices) expect(m.io.commit(i).rf_w, false)
  for (i <- m.io.unreserved_head.indices) {
    expect(m.io.unreserved_head(i).valid, true, "4進める")
    expect(m.io.unreserved_head(i).bits, 4+i, "4進める")
  }
  poke(m.io.used_num, 0)
  // rob(1)の命令をgraduate, rob(0)がまだなのでcommitは不可能
  for (i <- Seq(0, 1, 3)) {
    poke(m.io.graduate(i).valid, false)
    poke(m.io.graduate(i).bits.mispredicted, false)
  }
  // rob(1)の命令をgraduate, rob(0)がまだなのでcommitは不可能
  poke(m.io.graduate(2).valid, true)
  poke(m.io.graduate(2).bits.mispredicted, false)
  poke(m.io.graduate(2).bits.inst_info.rf_w, true)
  poke(m.io.graduate(2).bits.addr, 1)
  poke(m.io.graduate(2).bits.inst_info.rd_addr, 1)
  poke(m.io.graduate(2).bits.data, 10)
  step(1) // ------------------------------------------------------------

  for (i <- m.io.commit.indices) expect(m.io.commit(i).rf_w, false)
  expect(m.io.unreserved_head(0).valid, true)
  expect(m.io.unreserved_head(0).bits, 4)
  poke(m.io.used_num, 3)
  for (i <- Seq(1, 2, 3)) {
    poke(m.io.graduate(i).valid, false)
    poke(m.io.graduate(i).bits.mispredicted, false)
  }
  // rob(0)の命令をgraduate, これでrob(1)以下の命令をcommit可能に
  poke(m.io.graduate(0).valid, true)
  poke(m.io.graduate(0).bits.mispredicted, false)
  poke(m.io.graduate(0).bits.inst_info.rf_w, true)
  poke(m.io.graduate(0).bits.addr, 0)
  poke(m.io.graduate(0).bits.inst_info.rd_addr, 2)
  poke(m.io.graduate(0).bits.data, 20)
  step(1) // ------------------------------------------------------------

  poke(m.io.used_num, 0)
  poke(m.io.graduate(0).valid, true)
  poke(m.io.graduate(0).bits.mispredicted, true)
  poke(m.io.graduate(0).bits.addr, 6)
  poke(m.io.graduate(0).bits.mispredicted, true)
  poke(m.io.graduate(0).bits.mispredicted, true)
  for (i <- Seq(1, 2, 3)) {
    poke(m.io.graduate(i).valid, false)
    poke(m.io.graduate(i).bits.mispredicted, false)
  }
  expect(m.io.commit(0).rf_w, true)
  expect(m.io.commit(0).rd_addr, 2)
  expect(m.io.commit(0).data, 20)
  expect(m.io.commit(1).rf_w, true)
  expect(m.io.commit(1).data, 10)
  expect(m.io.commit(1).rd_addr, 1)
  expect(m.io.commit(2).rf_w, false)
  expect(m.io.commit(3).rf_w, false)

  expect(m.io.unreserved_head(0).valid, false, "分岐予測失敗時はrestoreするためにreserveはお休み")
  expect(m.io.unreserved_head(0).bits, 7)

  step(1) // ------------------------------------------------------------

  // TODO: mispredicted時のテストを書く
  poke(m.io.used_num, 0)
  for (i <- Seq(0, 1, 2, 3)) {
    poke(m.io.graduate(i).valid, false)
    poke(m.io.graduate(i).bits.mispredicted, false)
  }

  for (i <- 0 until 4)
    expect(m.io.commit(i).rf_w, false)

  expect(m.io.unreserved_head(0).valid, true, "分岐予測失敗後のクロック")
  expect(m.io.unreserved_head(0).bits, 6, "分岐予測失敗後のクロック")

  step(1) // ------------------------------------------------------------
}
*/
