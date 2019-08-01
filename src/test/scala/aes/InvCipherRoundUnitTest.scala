package aes

import chisel3.iotesters
import chisel3.iotesters.{ChiselFlatSpec, Driver, PeekPokeTester}

class InvCipherRoundUnitTester(c: InvCipherRound, transform: String, InvSubBytes_SCD: Boolean) extends PeekPokeTester(c) {
  require(transform == "AddRoundKeyOnly" || transform == "NoInvMixColumns" || transform == "CompleteRound")

  private val aes_icipher_round = c

  val state_in = Array(0x32, 0x43, 0xf6, 0xa8, 0x88, 0x5a, 0x30, 0x8d, 0x31, 0x31, 0x98, 0xa2, 0xe0, 0x37, 0x07, 0x34)
  val roundKey = Array(0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0a, 0x0b, 0x0c, 0x0d, 0x0e, 0x0f)

  val state_out_ARK = Array(0x32, 0x42, 0xf4, 0xab, 0x8c, 0x5f, 0x36, 0x8a, 0x39, 0x38, 0x92, 0xa9, 0xec, 0x3a, 0x9, 0x3b)
  val state_out_NMC = Array(0xa1, 0xb3, 0xe0, 0xb7, 0x93, 0x61, 0x3e, 0x1d, 0x26, 0x4f, 0xdc, 0x23, 0xac, 0x23, 0x6, 0x60)
  val state_out_C = Array(0xc1, 0x5e, 0xa, 0xd0, 0xed, 0x37, 0xf9, 0xf2, 0x7b, 0x93, 0xb6, 0xc8, 0xd7, 0x9e, 0xb4, 0x14)

  val state_out = transform match {
    case "AddRoundKeyOnly" => state_out_ARK
    case "NoInvMixColumns" => state_out_NMC
    case "CompleteRound" => state_out_C
  }

  step(4) // test that things are fine in Idle state

  // send the ciphertext
  for (i <- 0 until Params.StateLength) {
    poke(aes_icipher_round.io.state_in(i), state_in(i))
  }
  // send the round key
  for (j <- 0 until Params.StateLength)
    poke(aes_icipher_round.io.roundKey(j), roundKey(j))
  step(1)

  // check output
  for (i <- 0 until Params.StateLength)
    expect(aes_icipher_round.io.state_out(i), state_out(i))

  step(4)
}

// Run test with:
// sbt 'testOnly aes.InvCipherRoundTester'
// extend with the option '-- -z verbose' or '-- -z vcd' for specific test

class InvCipherRoundTester extends ChiselFlatSpec {

  private val transform = "CompleteRound" // AddRoundKeyOnly NoInvMixColumns CompleteRound
  private val InvSubBytes_SCD = false
  private val backendNames = Array("firrtl", "verilator")
  private val dir = "InvCipherRound"

  for (backendName <- backendNames) {
    "Inverse Cipher" should s"execute AES Inverse Cipher (with $backendName)" in {
      Driver(() => new InvCipherRound(transform, InvSubBytes_SCD), backendName) {
        c => new InvCipherRoundUnitTester(c, transform, InvSubBytes_SCD)
      } should be(true)
    }
  }

  "Basic test using Driver.execute" should "be used as an alternative way to run specification" in {
    iotesters.Driver.execute(
      Array("--target-dir", "test_run_dir/" + dir + "_basic_test", "--top-name", dir),
      () => new InvCipherRound(transform, InvSubBytes_SCD)) {
      c => new InvCipherRoundUnitTester(c, transform, InvSubBytes_SCD)
    } should be(true)
  }

  "using --backend-name verilator" should "be an alternative way to run using verilator" in {
    if (backendNames.contains("verilator")) {
      iotesters.Driver.execute(
        Array("--target-dir", "test_run_dir/" + dir + "_verilator_test", "--top-name", dir,
          "--backend-name", "verilator"), () => new InvCipherRound(transform, InvSubBytes_SCD)) {
        c => new InvCipherRoundUnitTester(c, transform, InvSubBytes_SCD)
      } should be(true)
    }
  }

  "using --backend-name firrtl" should "be an alternative way to run using firrtl" in {
    if (backendNames.contains("firrtl")) {
      iotesters.Driver.execute(
        Array("--target-dir", "test_run_dir/" + dir + "_firrtl_test", "--top-name", dir,
          "--backend-name", "firrtl", "--generate-vcd-output", "on"), () => new InvCipherRound(transform, InvSubBytes_SCD)) {
        c => new InvCipherRoundUnitTester(c, transform, InvSubBytes_SCD)
      } should be(true)
    }
  }

  "running with --is-verbose" should "show more about what's going on in your tester" in {
    iotesters.Driver.execute(
      Array("--target-dir", "test_run_dir/" + dir + "_verbose_test", "--top-name", dir,
        "--is-verbose"), () => new InvCipherRound(transform, InvSubBytes_SCD)) {
      c => new InvCipherRoundUnitTester(c, transform, InvSubBytes_SCD)
    } should be(true)
  }

}