package aes

import chisel3.iotesters
import chisel3.iotesters.{ChiselFlatSpec, Driver, PeekPokeTester}

class CipherRoundUnitTester(c: CipherRound, transform: String, SubBytes_SCD: Boolean) extends PeekPokeTester(c) {
  require(transform == "AddRoundKeyOnly" || transform == "NoMixColumns" || transform == "CompleteRound")

  private val aes_cipher_round = c

  val state_in = Array(0x32, 0x43, 0xf6, 0xa8, 0x88, 0x5a, 0x30, 0x8d, 0x31, 0x31, 0x98, 0xa2, 0xe0, 0x37, 0x07, 0x34)
  val roundKey = Array(0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0a, 0x0b, 0x0c, 0x0d, 0x0e, 0x0f)

  val state_out_ARK = Array(0x32, 0x42, 0xf4, 0xab, 0x8c, 0x5f, 0x36, 0x8a, 0x39, 0x38, 0x92, 0xa9, 0xec, 0x3a, 0x9, 0x3b)
  val state_out3_NMC = Array(0x23, 0xbf, 0x44, 0x1b, 0xc0, 0xc2, 0xc3, 0xc5, 0xcf, 0x93, 0x48, 0x56, 0xed, 0x17, 0xa, 0x35)
  val state_out2_C = Array(0xc1, 0x97, 0x3b, 0xae, 0xc2, 0xc2, 0xc9, 0xcd, 0x37, 0x7a, 0x34, 0x3b, 0xc5, 0xee, 0xb3, 0x5d)

  val state_out = transform match {
    case "AddRoundKeyOnly" => state_out_ARK
    case "NoMixColumns" => state_out3_NMC
    case "CompleteRound" => state_out2_C
  }

  step(4) // test that things are fine in Idle state

  // send the plaintext
  for (i <- 0 until Params.StateLength) {
    poke(aes_cipher_round.io.state_in(i), state_in(i))
  }
  // send the round key
  for (j <- 0 until Params.StateLength)
    poke(aes_cipher_round.io.roundKey(j), roundKey(j))
  step(1)

  for (i <- 0 until Params.StateLength)
    expect(aes_cipher_round.io.state_out(i), state_out(i))

  step(4)
}

// Run test with:
// sbt 'testOnly aes.CipherRoundTester'
// extend with the option '-- -z verbose' or '-- -z vcd' for specific test

class CipherRoundTester extends ChiselFlatSpec {

  private val transform = "CompleteRound" // AddRoundKeyOnly NoMixColumns CompleteRound
  private val SubBytes_SCD = false
  private val backendNames = Array("firrtl", "verilator")
  private val dir = "CipherRound"

  for (backendName <- backendNames) {
    "CipherRound" should s"execute AES Cipher Round (with $backendName)" in {
      Driver(() => new CipherRound(transform, SubBytes_SCD), backendName) {
        c => new CipherRoundUnitTester(c, transform, SubBytes_SCD)
      } should be(true)
    }
  }

  "Basic test using Driver.execute" should "be used as an alternative way to run specification" in {
    iotesters.Driver.execute(
      Array("--target-dir", "test_run_dir/" + dir + "_basic_test", "--top-name", dir),
      () => new CipherRound(transform, SubBytes_SCD)) {
      c => new CipherRoundUnitTester(c, transform, SubBytes_SCD)
    } should be(true)
  }

  "using --backend-name verilator" should "be an alternative way to run using verilator" in {
    if (backendNames.contains("verilator")) {
      iotesters.Driver.execute(
        Array("--target-dir", "test_run_dir/" + dir + "_verilator_test", "--top-name", dir,
          "--backend-name", "verilator"), () => new CipherRound(transform, SubBytes_SCD)) {
        c => new CipherRoundUnitTester(c, transform, SubBytes_SCD)
      } should be(true)
    }
  }

  "using --backend-name firrtl" should "be an alternative way to run using firrtl" in {
    if (backendNames.contains("firrtl")) {
      iotesters.Driver.execute(
        Array("--target-dir", "test_run_dir/" + dir + "_firrtl_test", "--top-name", dir,
          "--backend-name", "firrtl", "--generate-vcd-output", "on"), () => new CipherRound(transform, SubBytes_SCD)) {
        c => new CipherRoundUnitTester(c, transform, SubBytes_SCD)
      } should be(true)
    }
  }

  "running with --is-verbose" should "show more about what's going on in your tester" in {
    iotesters.Driver.execute(
      Array("--target-dir", "test_run_dir/" + dir + "_verbose_test", "--top-name", dir,
        "--is-verbose"), () => new CipherRound(transform, SubBytes_SCD)) {
      c => new CipherRoundUnitTester(c, transform, SubBytes_SCD)
    } should be(true)
  }

}