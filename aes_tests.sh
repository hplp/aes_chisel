# script to run all tests at once

echo "SubBytesTester"
sbt 'testOnly aes.SubBytesTester'

echo "ShiftRowsTester"
sbt 'testOnly aes.ShiftRowsTester'

echo "MixColumnsTester"
sbt 'testOnly aes.MixColumnsTester'

echo "InvSubBytesTester"
sbt 'testOnly aes.InvSubBytesTester'

echo "InvShiftRowsTester"
sbt 'testOnly aes.InvShiftRowsTester'

echo ""
sbt 'testOnly aes.InvMixColumnsTester'

echo "InvMixColumnsTester"
sbt 'testOnly aes.AddRoundKeyTester'

echo "CipherTester"
sbt 'testOnly aes.CipherTester'

echo "InvCipherTester"
sbt 'testOnly aes.InvCipherTester'

echo "AESTester"
sbt 'testOnly aes.AESTester'


