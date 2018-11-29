package aes

import chisel3.iotesters
import chisel3.iotesters.{ChiselFlatSpec, Driver, PeekPokeTester}

class InvShiftRowsUnitTester(c: InvShiftRows) extends PeekPokeTester(c) {

  def computeInvShiftRows(state_in: Array[Int]): Array[Int] = {
    var state_out = new Array[Int](Params.StateLength)

    state_out(0) = state_in(0)
    state_out(1) = state_in(13)
    state_out(2) = state_in(10)
    state_out(3) = state_in(7)

    state_out(4) = state_in(4)
    state_out(5) = state_in(1)
    state_out(6) = state_in(14)
    state_out(7) = state_in(11)

    state_out(8) = state_in(8)
    state_out(9) = state_in(5)
    state_out(10) = state_in(2)
    state_out(11) = state_in(15)

    state_out(12) = state_in(12)
    state_out(13) = state_in(9)
    state_out(14) = state_in(6)
    state_out(15) = state_in(3)

    state_out
  }

  private val aes_isr = c
  var state = Array(0x32, 0x43, 0xf6, 0xa8, 0x88, 0x5a, 0x30, 0x8d, 0x31, 0x31, 0x98, 0xa2, 0xe0, 0x37, 0x07, 0x34)

  for (i <- 0 until Params.StateLength)
    poke(aes_isr.io.state_in(i), state(i))
  step(1)

  state = computeInvShiftRows(state)
  println(state.deep.mkString(" "))

  for (i <- 0 until Params.StateLength)
    expect(aes_isr.io.state_out(i), state(i))
}

// Run test with:
// sbt 'testOnly aes.InvShiftRowsTester'
// extend with the option '-- -z verbose' or '-- -z vcd' for specific test

class InvShiftRowsTester extends ChiselFlatSpec {

  private val backendNames = Array("firrtl", "verilator")
  private val dir = "InvShiftRows"

  for (backendName <- backendNames) {
    "InvShiftRows" should s"execute AES InvShiftRows (with $backendName)" in {
      Driver(() => new InvShiftRows, backendName) {
        c => new InvShiftRowsUnitTester(c)
      } should be(true)
    }
  }

  "Basic test using Driver.execute" should "be used as an alternative way to run specification" in {
    iotesters.Driver.execute(
      Array("--target-dir", "test_run_dir/" + dir + "_basic_test", "--top-name", dir), () => new InvShiftRows) {
      c => new InvShiftRowsUnitTester(c)
    } should be(true)
  }

  "using --backend-name verilator" should "be an alternative way to run using verilator" in {
    if (backendNames.contains("verilator")) {
      iotesters.Driver.execute(
        Array("--target-dir", "test_run_dir/" + dir + "_verilator_test", "--top-name", dir,
          "--backend-name", "verilator"), () => new InvShiftRows) {
        c => new InvShiftRowsUnitTester(c)
      } should be(true)
    }
  }

  "using --backend-name firrtl" should "be an alternative way to run using firrtl" in {
    if (backendNames.contains("firrtl")) {
      iotesters.Driver.execute(
        Array("--target-dir", "test_run_dir/" + dir + "_firrtl_test", "--top-name", dir,
          "--backend-name", "firrtl", "--generate-vcd-output", "on"), () => new InvShiftRows) {
        c => new InvShiftRowsUnitTester(c)
      } should be(true)
    }
  }

  "running with --is-verbose" should "show more about what's going on in your tester" in {
    iotesters.Driver.execute(
      Array("--target-dir", "test_run_dir/" + dir + "_verbose_test", "--top-name", dir,
        "--is-verbose"), () => new InvShiftRows) {
      c => new InvShiftRowsUnitTester(c)
    } should be(true)
  }

}
