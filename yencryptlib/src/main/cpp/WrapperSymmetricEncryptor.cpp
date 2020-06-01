#include "WrapperSymmetricEncryptor.h"
#include "CRandomOpenSSL.h"
#include "CYException.h"
YSecurity::WrapperSymmetricEncryptor::WrapperSymmetricEncryptor()
{
	encryptor = new CYSymmetricOpenSSLEncypt();
}

YSecurity::yBytes * YSecurity::WrapperSymmetricEncryptor::encryptData(CYKey * key, yBytes * data)
{
	yUint lenIV = encryptor->getLenIV();
	yUint blockSize = encryptor->getBlockSize();
	
	CYData<ubyte> iv(lenIV);
	
	CRandomOpenSSL::getBytes(iv.getData(), iv.getLenData());

	//CYData<ubyte> dataCipher();

	yUint len = data->getLenData() + blockSize * 3;
	CYData<ubyte> dataCipher(len);

	if (!encryptor->encryptData(data->getData(), data->getLenData(), dataCipher.getData(), len, key->getKeyBytes()->getData(), iv.getData()))
	{
		throw CYEncryptException("Error. Encrypt. Process.");
	}

	auto id = key->getId();

	unsigned int resLen = sizeof(id) + sizeof(yUint)*2 + iv.getLenData() + len;

	CYData<ubyte> * res = new CYData<ubyte>(resLen);

	unsigned int pos = 0;

	res->past(pos, (yByte*)&id, sizeof(id));
	pos += sizeof(id);
	res->past(pos, (yByte*)&lenIV, sizeof(lenIV));
	pos += sizeof(lenIV);
	res->past(pos, iv);
	pos += lenIV;
	res->past(pos, (yByte*)&len, sizeof(len));
	pos += sizeof(len);
	res->past(pos, dataCipher.getData(), len);
	pos += len;

	if (pos!=res->getLenData())
	{
		delete res;
		throw CYEncryptException("Error. Encrypt. Format");
	}

	return res;
}

YSecurity::yBytes * YSecurity::WrapperSymmetricEncryptor::decryptData(CYKey * key, yBytes * data)
{
	yUint blockSize = encryptor->getBlockSize();

	yBigUint id = 0;

	yUint pos = 0;
	yUint ivLen = 0;
	yUint dataLen = 0;
	memcpy(&id, &data->getData()[pos], sizeof(id));
	pos += sizeof(id);
	
	if (key->getId() != id)
	{
		throw CYEncryptException("Error. Dercrypt. Error key.");
	}

	memcpy(&ivLen, &data->getData()[pos], sizeof(ivLen));
	pos += sizeof(ivLen);
	pos += ivLen;

	memcpy(&dataLen, &data->getData()[pos], sizeof(dataLen));
	pos += sizeof(dataLen);

	yUint len = dataLen + 3 * blockSize;
	CYData<ubyte> dataOpen(len);

	if (!encryptor->decryptData(&data->getData()[pos], dataLen, dataOpen.getData(), len, 
		key->getKeyBytes()->getData(), &data->getData()[sizeof(id)+sizeof(ivLen)]))
	{
		throw CYEncryptException("Error. Dercrypt. Process.");
	}


	return new yBytes(dataOpen.getData(), len);
}

YSecurity::WrapperSymmetricEncryptor::~WrapperSymmetricEncryptor()
{
	delete encryptor;
}
