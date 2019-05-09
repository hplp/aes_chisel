package aes

import chisel3._
import chisel3.util._
import chisel3.util.log2Ceil
import chisel3.util.Cat

// implements wrapper for AES cipher and inverse cipher
// change Nk=4 for AES128, NK=6 for AES192, Nk=8 for AES256
// change expandedKeyMemType= ROM, Mem, SyncReadMem
class AES(Nk: Int, unrolled: Boolean, SubBytes_SCD: Boolean, InvSubBytes_SCD: Boolean, expandedKeyMemType: String) extends Module {
  require(Nk == 4 || Nk == 6 || Nk == 8)
  require(expandedKeyMemType == "ROM" || expandedKeyMemType == "Mem" || expandedKeyMemType == "SyncReadMem")
  val KeyLength: Int = Nk * Params.rows
  val Nr: Int = Nk + 6 // 10, 12, 14 rounds
  val Nrplus1: Int = Nr + 1 // 10+1, 12+1, 14+1
  val EKDepth: Int = 16 // enough memory for any expanded key

  val io = IO(new Bundle {
    val AES_mode = Input(UInt(2.W)) //  0=00=off, 1=01=expanded key update, 2=10=cipher, 3=11=inverse cipher
    val start = Input(Bool())
    //
    val input_text = Input(Vec(Params.StateLength, UInt(8.W))) // plaintext, ciphertext, roundKey
    //
    val output_text = Output(Vec(Params.StateLength, UInt(8.W))) // ciphertext or plaintext
    val output_valid = Output(Bool())
  })

  val initValues = Seq.fill(Params.StateLength) {
    0.U(8.W)
  }

  // Instantiate module objects
  val CipherModule = Cipher(Nk, SubBytes_SCD)
  val InvCipherModule = InvCipher(Nk, InvSubBytes_SCD)

  // ROM = configuration that stores the expanded key in ROM
  val ROMeKeyOut = RegInit(VecInit(initValues))

  //TODO: update this Mem implementation to make use of Masks

  // A roundKey is Params.StateLength bytes, and 1+(10/12/14) (< EKDepth) of them are needed
  // Mem = combinational/asynchronous-read, sequential/synchronous-write = register banks
  // Create a asynchronous-read, synchronous-write memory block big enough for any key length
  val expandedKeyARMem = Mem(EKDepth, Vec(Params.StateLength, UInt(8.W)))


  // SyncReadMem = sequential/synchronous-read, sequential/synchronous-write = SRAMs
  // Create a synchronous-read, synchronous-write memory block big enough for any key length
  val expandedKeySRMem = SyncReadMem(EKDepth, Vec(Params.StateLength, UInt(8.W)))

  // use the same address and dataOut val elements to interface with the parameterized memory
  val address = RegInit(0.U(log2Ceil(EKDepth).W))
  val dataOut = Wire(Vec(Params.StateLength, UInt(8.W)))

  when(io.AES_mode === 1.U) { // write to memory
    if (expandedKeyMemType == "Mem") {
      expandedKeyARMem(address) := io.input_text
    }
    else if (expandedKeyMemType == "SyncReadMem") {
      expandedKeySRMem(address) := io.input_text
    }
    dataOut := DontCare
    address := address + 1.U
  }
    .otherwise { // read from memory
      if (expandedKeyMemType == "Mem") {
        dataOut := expandedKeyARMem(address)
      }
      else if (expandedKeyMemType == "SyncReadMem") {
        dataOut := expandedKeySRMem(address)
      }
      else if (expandedKeyMemType == "ROM") {
        dataOut := DontCare
      }

      // address logistics
      when(io.AES_mode === 2.U) {
        address := address + 1.U
      }
        .elsewhen(io.AES_mode === 3.U) {
          when(address === 0.U) {
            address := Nr.U
          }
            .otherwise {
              address := address - 1.U
            }
        }
        .otherwise {
          address := 0.U
        }
    }

  if (expandedKeyMemType == "ROM") {
    Nk match {
      case 4 => ROMeKeyOut := ROMeKeys.expandedKey128(address)
      case 6 => ROMeKeyOut := ROMeKeys.expandedKey192(address)
      case 8 => ROMeKeyOut := ROMeKeys.expandedKey256(address)
    }
  }

  // The roundKey for each round can go to both the cipher and inverse cipher (for now TODO)
  if (expandedKeyMemType == "Mem" || expandedKeyMemType == "SyncReadMem") {
    CipherModule.io.roundKey := dataOut
    InvCipherModule.io.roundKey := dataOut
  } else if (expandedKeyMemType == "ROM") {
    CipherModule.io.roundKey := ROMeKeyOut
    InvCipherModule.io.roundKey := ROMeKeyOut
  }

  // The input text can go to both the cipher and the inverse cipher (for now TODO)
  CipherModule.io.plaintext <> io.input_text
  InvCipherModule.io.ciphertext <> io.input_text

  // Cipher starts at (start=1 and AES_Mode=2)
  CipherModule.io.start := io.start && (io.AES_mode === 2.U)
  // Inverse Cipher starts at (start=1 and AES_Mode=1)
  InvCipherModule.io.start := io.start && (io.AES_mode === 3.U)

  // AES output_valid can be the Cipher.output_valid OR InvCipher.output_valid
  io.output_valid := CipherModule.io.state_out_valid || InvCipherModule.io.state_out_valid
  // AES output can be managed using a Mux on the Cipher output and the InvCipher output
  io.output_text <> Mux(CipherModule.io.state_out_valid, CipherModule.io.state_out, InvCipherModule.io.state_out)

  // Debug statements
  //printf("address=%d, rom_dataOut=%x%x%x%x%x%x%x%x%x%x%x%x%x%x%x%x \n", address, ROMeKeyOut(0), ROMeKeyOut(1), ROMeKeyOut(2), ROMeKeyOut(3), ROMeKeyOut(4), ROMeKeyOut(5), ROMeKeyOut(6), ROMeKeyOut(7), ROMeKeyOut(8), ROMeKeyOut(9), ROMeKeyOut(10), ROMeKeyOut(11), ROMeKeyOut(12), ROMeKeyOut(13), ROMeKeyOut(14), ROMeKeyOut(15))
  //printf("address=%d, mem_dataOut=%x \n", address, dataOut)
  //printf("AES mode=%b start=%b, mem_address=%d, mem_dataOut=%x \n", io.AES_mode, io.start, address, dataOut)
}

object AES {
  def apply(Nk: Int, unrolled: Boolean, SubBytes_SCD: Boolean, InvSubBytes_SCD: Boolean, expandedKeyMemType: String): AES = Module(new AES(Nk, unrolled, SubBytes_SCD, InvSubBytes_SCD, expandedKeyMemType))
}