#include "CYSecurityFacad.h"
#include "CRandomOpenSSL.h"
#include "CYException.h"
#include "SymmetricEncrypt.h"
#include "CSymmetricOpenSSLGeneratorFromPassword.h"
#include "COpenSSLRSAEncryptAsymmetricKeysGenerator.h"
#include "CSignOpenSSLRSA.h"
#include "CVerifyOpenSSLRSA.h"
#include "COpenSSLRSAEncryptor.h"
#include "COpenSSLRSADecryptor.h"
#include "YTime.h"
#include "WrapperSymmetricEncryptor.h"
#include <fstream>
#include <memory>


bool YSecurity::CYSecurityFacad::isInit = false;

YSecurity::CYMetaMsg * YSecurity::CYSecurityFacad::getMetaData(YSecurity::CYData<YSecurity::ubyte>& msg)
{
	return new CYMetaMsg(msg);
}

void YSecurity::CYSecurityFacad::init(YSecurity::CYData<YSecurity::ubyte> seed)
{
	CYSecurityFacad::isInit = true;
	YSecurity::CRandomOpenSSL::seed(seed.getData(), seed.getLenData());
}

void YSecurity::CYSecurityFacad::addRandom(YSecurity::CYData<YSecurity::ubyte> seed)
{
	YSecurity::CRandomOpenSSL::addRandom(seed.getData(), seed.getLenData());
}

void YSecurity::CYSecurityFacad::setEmptyKeys()
{
	symmetricEncryptKey = nullptr;

	publicEncryptKeyToSend = nullptr;
	privateSignKeyToSend = nullptr;

	privateEncryptKeyToReceive = nullptr;
	publicSignKeyToReceive = nullptr;
}

YSecurity::CYData<YSecurity::ubyte> YSecurity::CYSecurityFacad::randomPaddingByte(YSecurity::uint_32 sizeData)
{
	srand((uint_32)time(0));

	YSecurity::uint_32 len = minRandomLen + (rand()%(maxRandomLen - minRandomLen)) + 1;
	YSecurity::CYData<YSecurity::ubyte> t (len + sizeof(len));
	t.past(0, (YSecurity::ubyte*)&len, sizeof(len));
	YSecurity::CRandomOpenSSL::getBytes(t[sizeof(len)], len);
	return t;
}

YSecurity::CYKeysPair * YSecurity::CYSecurityFacad::getAsymmetricKeys(YSecurity::uint_32 lenKey, YSecurity::uint_32 version,
	YSecurity::uint_64 id, YSecurity::uint_32 timeLife, bool isSign, const char * hash)
{
	if (!isInit)
		throw CYInitializationException();
	if (!password)
		throw CYPasswordException();

	COpenSSLRSAEncryptAsymmetricKeysGenerator aGen(lenKey);

	aGen.generate();
	if (!aGen.checkKeys())
	{
		throw CYGenerateKeyException("RSA key failed validation");
	}

	auto type = isSign ? YTypeKey::publicKeySign : YTypeKey::publicKeyEncrypt;
	auto algoName = isSign ? RSA_PSS_ALGO : RSA_OAEP_ALGO;

	CYKey * publicKey = new CYKey(version, id, (yChar*)algoName, strlen(algoName),
		(yChar*)hash, strlen(hash), aGen.getPublicKey(), timeLife, YSecurity::YTime::getDateTime(),
		lenKey, aGen.getLenBufferPublicKey(), false, type, nullptr, 0);

	type = isSign ? YTypeKey::privateKeySign : YTypeKey::privateKeyEncrypt;

	CYKey * privateKey = new CYKey(version, id, (yChar*)algoName, strlen(algoName),
		(yChar*)hash, strlen(hash), aGen.getPrivateKey(), timeLife, YSecurity::YTime::getDateTime(),
		lenKey, aGen.getLenBufferPrivateKey(), false, type, nullptr, 0);


	auto masterKey = getMasterKey();

	auto encryptedPrivateKey = privateKey->getEncryptedKey(masterKey);

	delete masterKey;
	delete privateKey;
	
	return new CYKeysPair(encryptedPrivateKey, publicKey);
}

YSecurity::CYData<YSecurity::ubyte> YSecurity::CYSecurityFacad::encryptDataSymmetricKey(YSecurity::CYData<YSecurity::ubyte>& data, YSecurity::CYKey * symmetricKey)
{
	//auto key = decryptKey(*symmetricKey->getKey());

	//return encryptDataSymmetricKey(data, key);
	return YSecurity::CYData<YSecurity::ubyte>();
}

YSecurity::CYData<YSecurity::ubyte> YSecurity::CYSecurityFacad::encryptDataSymmetricKey(YSecurity::CYData<YSecurity::ubyte>& data, YSecurity::CYData<YSecurity::ubyte>& key)
{
	CYSymmetricOpenSSLEncypt encript(SYMMETRIC_ALGORITHM);
	uint32_t lenKey = encript.getLenKey();
	uint32_t blockSize = encript.getBlockSize();

	YSecurity::CYData<YSecurity::ubyte> dataEncripted(((data.getLenData() / blockSize) + 2)*blockSize);

	if (!encript.encryptData(data.getData(), data.getLenData(), dataEncripted.getData(), dataEncripted.getLenData(), key[0], key[lenKey]))
	{
		throw CYEncryptException("Could not encrypt on symmetric key");
	}

	return dataEncripted;
}

YSecurity::CYData<YSecurity::ubyte> YSecurity::CYSecurityFacad::encryptDataAsymmetricKey(YSecurity::CYData<YSecurity::ubyte>& data)
{
	auto key = publicEncryptKeyToSend->getKeyBytes();

	COpenSSLRSAEncryptor encriptA(key->getData(), key->getLenData(), publicEncryptKeyToSend->getHashName());

	CYSymmetricOpenSSLEncypt encript(SYMMETRIC_ALGORITHM);

	yUint lenKey = encript.getLenKey();
	yUint lenIV = encript.getLenIV();
	yUint blockSize = encript.getBlockSize();

	yBytes keyIv(lenKey+lenIV);

	YSecurity::CRandomOpenSSL::getBytes(keyIv.getData(), keyIv.getLenData());

	yUint len = data.getLenData() + blockSize * 3;
	yBytes encryptedData(len);
	if (!encript.encryptData(data.getData(), data.getLenData(), encryptedData.getData(), len, &keyIv.getData()[0], &keyIv.getData()[lenKey]))
	{
		throw CYEncryptException("Could not encrypt");
	}

	encriptA.encryptData(keyIv.getData(), keyIv.getLenData());
	if (encriptA.isError())
	{
		throw CYEncryptException("Could not encrypt on asymmetric key");
	}

	yUint sizeKeyEnc = (yUint) encriptA.getLenEcryptedData();

	auto id = publicEncryptKeyToSend->getId();

	YSecurity::CYData<YSecurity::ubyte> encData(sizeKeyEnc + sizeof(sizeKeyEnc) + sizeof(id) + len + sizeof(len));
	YSecurity::total_size pos = 0;

	encData.past(pos, (YSecurity::ubyte *) &id, sizeof(id));
	pos += sizeof(id);

	encData.past(pos, (YSecurity::ubyte *) &sizeKeyEnc, sizeof(sizeKeyEnc));
	pos += sizeof(sizeKeyEnc);

	encData.past(pos, (YSecurity::ubyte *) encriptA.getEcryptedData(), sizeKeyEnc);
	pos += sizeKeyEnc;

	encData.past(pos, (YSecurity::ubyte *) &len, sizeof(len));
	pos += sizeof(len);

	encData.past(pos, (YSecurity::ubyte *) encryptedData.getData(), len);
	pos += len;


	if (pos != encData.getLenData())
	{
		throw CYEncryptException("Error. Asymmetic encryption. Pack error");
	}

	return encData;
}

YSecurity::CYData<YSecurity::ubyte> YSecurity::CYSecurityFacad::decryptDataAsymmetricKey(YSecurity::CYData<YSecurity::ubyte>& data)
{
	if (!privateEncryptKeyToReceive)
		throw CYNoKeyException("No private asymmetric encrypt key");

	/*if (!privateEncryptKeyToReceive->isAlive())
		throw CYTimeLife("The private asymmetric encrypt key lifetime has expired");*/

	auto master = getMasterKey(privateEncryptKeyToReceive->getSalt());
	auto masterKey = std::unique_ptr<CYKey>(master);

	auto key = privateEncryptKeyToReceive->getDecryptedKey(masterKey.get());
	auto decryptedKey = std::unique_ptr<CYKey>(key);

	auto keyBytes = decryptedKey->getKeyBytes();

	COpenSSLRSADecryptor decryptA(keyBytes->getData(), keyBytes->getLenData(), privateEncryptKeyToReceive->getHashName());

	yBigUint id;
	yUint pos = 0;
	data.copyBytes(pos, &id, sizeof(id));
	pos += sizeof(id);

	if (id != privateEncryptKeyToReceive->getId())
	{
		throw CYKeyException("id key != id key field");
	}

	yUint lenIvKeyEncrypted;

	data.copyBytes(pos, &lenIvKeyEncrypted, sizeof(lenIvKeyEncrypted));
	pos += sizeof(lenIvKeyEncrypted);

	yBytes keyIvEncrypted(lenIvKeyEncrypted);
	data.copyBytes(pos, keyIvEncrypted.getData(), lenIvKeyEncrypted);
	pos += lenIvKeyEncrypted;

	yUint lenData;
	data.copyBytes(pos, &lenData, sizeof(lenData));
	pos += sizeof(lenData);

	decryptA.decryptData(keyIvEncrypted.getData(), lenIvKeyEncrypted);
	if (decryptA.isError())
	{
		throw CYEncryptException("Could not decrypt on asymmetric key");
	}

	yBytes keyIvDecrypted(decryptA.getDecryptedData(), decryptA.getLenDecryptedData());

	CYSymmetricOpenSSLEncypt decryptor(SYMMETRIC_ALGORITHM);

	yUint lenKey = decryptor.getLenKey();
	yUint blockSize = decryptor.getBlockSize();

	yUint lenOpenData = lenData + 3 * blockSize;
	yBytes openData(lenOpenData);

	if (!decryptor.decryptData(&data.getData()[pos], lenData, openData.getData(), lenOpenData, keyIvDecrypted.getData(), &keyIvDecrypted.getData()[lenKey]))
	{
		throw CYEncryptException("Could not decrypt on symmetic key");
	}

	return yBytes(openData.getData(), lenOpenData);
}



YSecurity::CYData<YSecurity::ubyte> YSecurity::CYSecurityFacad::packData(YSecurity::CYData<ubyte>& data)
{
	YSecurity::CYData<YSecurity::ubyte> randomByte = randomPaddingByte(data.getSizeInBytes());

	randomByte.add(data);

	return randomByte;
}

YSecurity::CYData<YSecurity::ubyte> YSecurity::CYSecurityFacad::unpackData(YSecurity::CYData<YSecurity::ubyte>& data)
{
	yUint lenRandom;
	yUint pos=0;
	data.copyBytes(pos, &lenRandom, sizeof(lenRandom));
	pos += sizeof(lenRandom);

	if (lenRandom > data.getLenData() - pos)
	{
		throw CYDataException("The length of a random sequence is greater than the length of the data block.");
	}
	pos += lenRandom;

	yUint len = data.getLenData() - pos;

	return yBytes(&data.getData()[pos], len);
}

YSecurity::CYData<YSecurity::ubyte> YSecurity::CYSecurityFacad::signDate(YSecurity::CYData<YSecurity::ubyte>& data)
{
	if (!privateSignKeyToSend)
		throw CYNoKeyException("No public asymmetric sign key");

	if (!privateSignKeyToSend->isAlive())
		throw CYTimeLife("The public asymmetric sign key lifetime has expired");


	auto masterKey = getMasterKey(privateSignKeyToSend->getSalt());
	auto uPtrMasterKey = std::unique_ptr<CYKey>(masterKey);

	auto signKey = privateSignKeyToSend->getDecryptedKey(masterKey);
	auto uPtrSignKey = std::unique_ptr<CYKey>(signKey);

	auto keyBytes = signKey->getKeyBytes();

	YSecurity::CSignOpenSSLRSA cSign(keyBytes->getData(), keyBytes->getLenData(), signKey->getHashName());

	cSign.addMsg(data.getData(), data.getLenData());
	cSign.subscribe();

	if (cSign.isError())
		throw CYSignException("Failed to sign the message");

	YSecurity::CYData<YSecurity::ubyte> sign(cSign.getSign(), cSign.getLenSign());

	return sign;
}

bool YSecurity::CYSecurityFacad::verifyData(YSecurity::CYData<YSecurity::ubyte>& data, YSecurity::CYData<YSecurity::ubyte>& signature)
{
	if (!publicSignKeyToReceive)
		throw CYNoKeyException("No public asymmetric sign key");

	/*if (!publicSignKeyToReceive->isAlive())
		throw CYTimeLife("The public asymmetric sign key lifetime has expired");*/

	auto key = publicSignKeyToReceive->getKeyBytes();

	CVerifyOpenSSLRSA cVerify(signature.getData(), signature.getSizeInBytes(), key->getData(), key->getSizeInBytes(), publicSignKeyToReceive->getHashName());

	cVerify.addMsg(data.getData(), data.getLenData());
	cVerify.verify();

	if (cVerify.isError())
		throw CYVerifityException("Verify failed");

	return cVerify.isCorrect();

}

YSecurity::CYData<YSecurity::ubyte> YSecurity::CYSecurityFacad::packMsg(
	YSecurity::uint_32 version, YSecurity::uint_32 type, YSecurity::uint_32 meta, YSecurity::uint_32 timeLife, YSecurity::date dateCreate,
	YSecurity::uint_64 idKey, YSecurity::CYData<YSecurity::ubyte>* data, YSecurity::CYData<YSecurity::ubyte>* signature)
{
	CYMsg msg(version, type, meta, timeLife, dateCreate, idKey, data, signature);
	return msg.getBytes();
}

YSecurity::CYMsg * YSecurity::CYSecurityFacad::unpackMsg(YSecurity::CYData<YSecurity::ubyte>& data)
{
	return new CYMsg(data);
}

YSecurity::CYSecurityFacad::CYSecurityFacad(): password(nullptr)
{
	setEmptyKeys();
	setRandomLen(1000, 10000);
}

YSecurity::CYSecurityFacad::CYSecurityFacad(YSecurity::CYData<YSecurity::char_t> & password, YSecurity::uint_64 id) : password(nullptr)
{
	this->setPassword(password);
	this->idMasterKey = id;
	setEmptyKeys();
	setRandomLen(1000, 10000);
}

YSecurity::CYSecurityFacad::~CYSecurityFacad()
{
	clear();
}

void YSecurity::CYSecurityFacad::clear()
{
	if (password)
		delete password;
}

void YSecurity::CYSecurityFacad::setPassword(YSecurity::CYData<YSecurity::char_t> & password)
{
	if (this->password)
	{
		delete this->password;
	}
	this->password = new YSecurity::CYData<YSecurity::char_t>(password);
}

void YSecurity::CYSecurityFacad::setRandomLen(YSecurity::uint_32 min, YSecurity::uint_32 max)
{
	this->minRandomLen = min;
	this->maxRandomLen = max;
}



YSecurity::CYKey * YSecurity::CYSecurityFacad::getSymmetricKey(YSecurity::uint_32 version, YSecurity::uint_64 id, YSecurity::uint_32 timeLife)
{
	if (!isInit)
		throw CYInitializationException();
	if (!password)
		throw CYPasswordException();

	CYSymmetricOpenSSLEncypt encript(SYMMETRIC_ALGORITHM);
	uint32_t lenKey = encript.getLenKey();

	YSecurity::CYData<YSecurity::ubyte> keyBytes(lenKey);
	YSecurity::CRandomOpenSSL::getBytes(keyBytes.getData(), keyBytes.getLenData());

	auto masterKey = getMasterKey();
	auto uPtrMasterKey = std::unique_ptr<CYKey>(masterKey);

	auto key = new YSecurity::CYKey(version, id, (char*)AES_CBC_ALGO, strlen(AES_CBC_ALGO),
		nullptr, 0, keyBytes.getData(), timeLife, YSecurity::YTime::getDateTime(),
		lenKey * 8, keyBytes.getLenData(), false, YSecurity::YTypeKey::secretKey, 
	nullptr, 0);

	auto uPtrKey = std::unique_ptr<CYKey>(key);

	auto keyEncrypted = key->getEncryptedKey(masterKey);


	return keyEncrypted;
}

YSecurity::CYKey * YSecurity::CYSecurityFacad::generateSymmetricKeyFromPassword(YSecurity::CYData<YSecurity::char_t> * password,
				YSecurity::CYData<YSecurity::ubyte> * salt, yBigUint idKey)
{
	CYSymmetricOpenSSLEncypt encript(SYMMETRIC_ALGORITHM);
	uint32_t lenKey = encript.getLenKey();

	YSecurity::CYData<YSecurity::ubyte> masterKey(lenKey);

	if(password!=NULL)
	{
		YSecurity::CYData<YSecurity::ubyte> saltKey(16);
		if (salt == nullptr)
			YSecurity::CRandomOpenSSL::getBytes(saltKey.getData(), saltKey.getLenData());
		else
		{
			if(salt->getLenData()!=16)
			{
				throw CYDataException("Lenght salt must be 16 bytes");
			}
			saltKey.past(0, *salt);
		}

		CSymmetricOpenSSLGeneratorFromPassword masterKeyGen(SHA_512, 100000);

		if (!masterKeyGen.GenereteKey((const ubyte*)password->getData(), password->getLenData(),
			saltKey.getData(), saltKey.getLenData(),
			masterKey.getLenData(), masterKey.getData()))
		{
			throw CYGenerateKeyException("Could not generate symmetric key from password");
		}

		return new YSecurity::CYKey(1, idKey, (char*)AES_CBC_ALGO, strlen(AES_CBC_ALGO),
			nullptr, 0, masterKey.getData(), 60, YSecurity::YTime::getDateTime(),
			lenKey * 8, masterKey.getLenData(), false, YSecurity::YTypeKey::secretKey, saltKey.getData(), saltKey.getLenData());

	}
	YSecurity::CRandomOpenSSL::getBytes(masterKey.getData(), masterKey.getLenData());

	return new YSecurity::CYKey(1, idKey, (char*)AES_CBC_ALGO, strlen(AES_CBC_ALGO),
			nullptr, 0, masterKey.getData(), 60, YSecurity::YTime::getDateTime(),
			lenKey * 8, masterKey.getLenData(), false, YSecurity::YTypeKey::secretKey, NULL, 0);

}

YSecurity::CYData<YSecurity::ubyte> * YSecurity::CYSecurityFacad::encryptData(YSecurity::uint_32 version,
				YSecurity::CYData<YSecurity::ubyte> & data, YSecurity::CYKey * key)
{
	if (!key)
		throw CYNoKeyException("No symmetric encryption key");

	WrapperSymmetricEncryptor encryptor;
	return encryptor.encryptData(key, &data);
}

YSecurity::CYData<YSecurity::ubyte> * YSecurity::CYSecurityFacad::decryptData(YSecurity::uint_32 version,
						YSecurity::CYData<YSecurity::ubyte> & data, YSecurity::CYKey * key)
{
	if (!key)
		throw CYNoKeyException("No symmetric decryption key");
	WrapperSymmetricEncryptor encryptor;
	return encryptor.decryptData(key, &data);
}


YSecurity::CYKey * YSecurity::CYSecurityFacad::getMasterKey(YSecurity::CYData<YSecurity::ubyte> * salt)
{
	return generateSymmetricKeyFromPassword(password, salt, idMasterKey);
}

YSecurity::CYKeysPair * YSecurity::CYSecurityFacad::getShortAsymmetricKeys(YSecurity::uint_32 version, YSecurity::uint_64 id, YSecurity::uint_32 timeLife, bool isSign, const char * hash)
{
	return getAsymmetricKeys(2048, version, id, timeLife, isSign, hash);
}

YSecurity::CYKeysPair * YSecurity::CYSecurityFacad::getMidleAsymmetricKeys(YSecurity::uint_32 version, YSecurity::uint_64 id, YSecurity::uint_32 timeLife, bool isSign, const char * hash)
{
	return getAsymmetricKeys(2048*2, version, id, timeLife, isSign, hash);
}

YSecurity::CYKeysPair * YSecurity::CYSecurityFacad::getLongAsymmetricKeys(YSecurity::uint_32 version, YSecurity::uint_64 id, YSecurity::uint_32 timeLife, bool isSign, const char * hash)
{
	return getAsymmetricKeys(2048*2*2, version, id, timeLife, isSign, hash);
}

YSecurity::CYData<YSecurity::ubyte> YSecurity::CYSecurityFacad::encryptSecretMsg(YSecurity::uint_32 version, YSecurity::uint_32 type, 
	YSecurity::uint_32 timeLife, YSecurity::CYData<YSecurity::ubyte> & secretMsg, YSecurity::uint_32 meta)
{
	if (!isInit)
		throw CYInitializationException();
	if (!password)
		throw CYPasswordException();
	if (!symmetricEncryptKey)
		throw CYNoKeyException("No symmetric encryption key");
	if (secretMsg.getLenData() == 0)
		throw CYDataException("Data of zero length cannot be encrypted");

	if (!symmetricEncryptKey->isAlive())
		throw CYTimeLife("The symmetric key lifetime has expired");

	auto masterKey = getMasterKey(symmetricEncryptKey->getSalt());
	auto uPtrMaster = std::unique_ptr<CYKey>(masterKey);
	auto key = symmetricEncryptKey->getDecryptedKey(masterKey);
	auto uPtrKey = std::unique_ptr<CYKey>(key);
	WrapperSymmetricEncryptor encryptor;

	auto openData = packData(secretMsg);
	auto encryptedData = encryptor.encryptData(key, &openData);
	auto uPtrEncryptedData = std::unique_ptr<yBytes>(encryptedData);

	auto encryptedMsg = packMsg(version, type, meta | 1, timeLife, YSecurity::YTime::getDateTime(), symmetricEncryptKey->getId(), encryptedData, 0);
	
	auto signature = signDate(encryptedMsg);

	auto signMsg = packMsg(version, type, meta | 3, timeLife, YSecurity::YTime::getDateTime(), privateSignKeyToSend->getId(), &encryptedMsg, &signature);

	return signMsg;
}

YSecurity::CYData<YSecurity::ubyte> YSecurity::CYSecurityFacad::encrypKeysMsg(YSecurity::uint_32 version, YSecurity::uint_32 timeLife, YSecurity::CYKey * key,
	YSecurity::uint_32 type, YSecurity::uint_32 meta)
{
	if (!isInit)
		throw CYInitializationException();
	if (!password)
		throw CYPasswordException();

	if (!publicEncryptKeyToSend)
		throw CYNoKeyException("No public asymmetric encrypt key");
	if (!privateSignKeyToSend)
		throw CYNoKeyException("No private asymmetric sign key");

	if (!publicEncryptKeyToSend->isAlive())
		throw CYTimeLife("The public asymmetric encrypt key lifetime has expired");

	if (!privateSignKeyToSend->isAlive())
		throw CYTimeLife("The private asymmetric sign key lifetime has expired");
		

	yBytes * keyData = nullptr;
	if (key->isEncryptedKey())
	{
		auto masterKey = getMasterKey(key->getSalt());

		keyData = key->getBytesDecryptedKeyData(masterKey);

		delete masterKey;
	}
	else
	{
		keyData = key->getBytesKeyData();
	}

	auto openData = packData(*keyData);
	delete keyData;
	auto cipherData = encryptDataAsymmetricKey(openData);
	auto encryptedMsg = packMsg(version, type, meta | 1, timeLife, YSecurity::YTime::getDateTime(), 
		this->publicEncryptKeyToSend->getId(), &cipherData, nullptr);

	auto signedData = signDate(encryptedMsg);

	auto sigMsg = packMsg(version, type, meta | 3, timeLife, YSecurity::YTime::getDateTime(),
		this->privateSignKeyToSend->getId(), &encryptedMsg, &signedData);

	return sigMsg;
	//return *key->getKeyBytes();
}

YSecurity::CYData<YSecurity::ubyte> YSecurity::CYSecurityFacad::signSecretMsg(YSecurity::uint_32 version, YSecurity::uint_32 type, 
	YSecurity::uint_32 timeLife, YSecurity::CYData<YSecurity::ubyte>& secretMsg, YSecurity::uint_32 meta)
{
	if (!isInit)
		throw CYInitializationException();
	if (!password)
		throw CYPasswordException();
	if (secretMsg.getLenData() == 0)
		throw CYDataException("Data of zero length cannot be encrypted");

	auto signature = signDate(secretMsg);

	auto signMsg = packMsg(version, type, meta | 2, timeLife, YSecurity::YTime::getDateTime(), privateSignKeyToSend->getId(), &secretMsg, &signature);

	return signMsg;

}

YSecurity::CYSecretMsg * YSecurity::CYSecurityFacad::decryptSecretMsg(YSecurity::CYData<YSecurity::ubyte>& secretMsg)
{
	if (!isInit)
		throw CYInitializationException();
	if (!password)
		throw CYPasswordException();
	if (!symmetricEncryptKey)
		throw CYNoKeyException("No symmetric encryption key");
	if (secretMsg.getLenData() == 0)
		throw CYDataException("Data of zero length cannot be decrypted");
	/*if (!symmetricEncryptKey->isAlive())
		throw CYTimeLife("The symmetric key lifetime has expired");*/
	
	auto msg = unpackMsg(secretMsg);
	auto uPtrMsg = std::unique_ptr<CYMsg>(msg);

	if (!msg->isEncryped() || !msg->isSigned())
	{
		throw CYDataException("Msg has unknown format. It is not encrypted or sign");
	}

	/*if (!msg->isAlive())
	{
		throw CYTimeLifeMsg("Msg sign timelife is up");
	}*/

	if (!publicSignKeyToReceive)
			throw CYNoKeyException("No asymmetric key for verify");


	if (msg->getIdKey() != publicSignKeyToReceive->getId())
	{
		throw CYKeyException("ID key != ID key field");
	}

	if (!YTime::isCorrectKey(msg->getDateCreate(), publicSignKeyToReceive->getDate(), publicSignKeyToReceive->getTimelife()))
	{
		throw CYTimeLife("This msg sign key with up timelife");
	}

	if (!verifyData(*msg->getData(), *msg->getSignature()))
	{
		throw CYVerifityException("Msg is not verify");
	}

	auto masterKey = getMasterKey(symmetricEncryptKey->getSalt());
	auto uPtrMasterKey = std::unique_ptr<CYKey>(masterKey);

	auto key = symmetricEncryptKey->getDecryptedKey(masterKey);
	auto uPtrKey = std::unique_ptr<CYKey>(key);

	auto encryptMsg = unpackMsg(*msg->getData());
	
	if (!encryptMsg->isEncryped())
	{
		throw CYDataException("This msg has unknow format");
	}

	/*if (!encryptMsg->isAlive())
	{
		throw CYTimeLifeMsg("Time life is up");
	}*/

	if (!YTime::isCorrectKey(encryptMsg->getDateCreate(), key->getDate(), key->getTimelife()))
	{
		throw CYTimeLife("This msg encrypt key with up timelife");
	}

	WrapperSymmetricEncryptor decryptor;
	auto decryptData = decryptor.decryptData(key, encryptMsg->getData());
	auto uPtrDecryptData = std::unique_ptr<yBytes>(decryptData);

	auto unpuckData = unpackData(*decryptData);


	return new CYSecretMsg(unpuckData, encryptMsg->getVer(), encryptMsg->getType(), encryptMsg->getMeta());
}

YSecurity::CYKey * YSecurity::CYSecurityFacad::decrypKeysMsg(YSecurity::CYData<YSecurity::ubyte> & keyMsg)
{
	if (!isInit)
		throw CYInitializationException();
	if (!password)
		throw CYPasswordException();

	auto res = unpackMsg(keyMsg);
	auto msg = std::unique_ptr<CYMsg>(res);

	if (!msg->isSigned())
	{
		throw CYDataException("This key msg is not sign");
	}

	if (!msg->isEncryped())
	{
		throw CYDataException("This key msg is not encrypted");
	}

	/*if (!msg->isAlive())
	{
		throw CYTimeLifeMsg("Key msg. Signature");
	}*/

	if (msg->getIdKey() != publicSignKeyToReceive->getId())
	{
		throw CYKeyException("Error key. ID sign key != set key ID");
	}

	if (!YTime::isCorrectKey(msg->getDateCreate(), publicSignKeyToReceive->getDate(), publicSignKeyToReceive->getTimelife()))
	{
		throw CYTimeLife("This msg sign key with up timelife");
	}

	if (!verifyData(*msg->getData(), *msg->getSignature()))
	{
		throw CYVerifityException("Key sign is not valid");
	}

	res = unpackMsg(*msg->getData());
	auto encyptedMsg = std::unique_ptr<CYMsg>(res);

	if (!encyptedMsg->isEncryped())
	{
		throw CYDataException("This key is non encrypted");
	}

	/*if (!encyptedMsg->isAlive())
	{
		throw CYTimeLifeMsg("Key msg. Encrypted msg");
	}*/

	if (!YTime::isCorrectKey(encyptedMsg->getDateCreate(), privateEncryptKeyToReceive->getDate(), privateEncryptKeyToReceive->getTimelife()))
	{
		throw CYTimeLife("This msg encrypt key with up timelife");
	}

	auto decryptedData = decryptDataAsymmetricKey(*encyptedMsg->getData());
	auto data = unpackData(decryptedData);

	CYKey openKey(data.getData(), data.getLenData());

	auto master = getMasterKey();
	auto masterKey = std::unique_ptr<CYKey>(master);

	return openKey.getEncryptedKey(masterKey.get());
}

YSecurity::CYSecretMsg * YSecurity::CYSecurityFacad::veryfiSecretMsg(YSecurity::CYData<YSecurity::ubyte>& secretMsg)
{
	if (!isInit)
		throw CYInitializationException();

	auto msg = unpackMsg(secretMsg);
	auto uPtrMsg = std::unique_ptr<CYMsg>(msg);

	if (!msg->isSigned())
	{
		throw CYDataException("Msg has unknown format. It is not sign");
	}

	/*if (!msg->isAlive())
	{
		throw CYTimeLifeMsg("Msg sign timelife is up");
	}*/

	if (msg->getIdKey() != publicSignKeyToReceive->getId())
	{
		throw CYKeyException("ID key != ID key field");
	}

	if (!YTime::isCorrectKey(msg->getDateCreate(), publicSignKeyToReceive->getDate(), publicSignKeyToReceive->getTimelife()))
	{
		throw CYTimeLife("This msg encrypt key with up timelife");
	}

	if (!verifyData(*msg->getData(), *msg->getSignature()))
	{
		throw CYVerifityException("Msg is not verify");
	}

	yBytes data(*msg->getData());
	return new CYSecretMsg(data, msg->getVer(), msg->getType(), msg->getMeta());
}

void YSecurity::CYSecurityFacad::setSymmetricEncryptKey(YSecurity::CYKey * key)
{
	if (!key)
	{
		throw CYDataException("It is not key for symmetric methods");
	}

	if (key && key->getType() != YTypeKey::secretKey)
	{
		throw CYDataException("It is not key for symmetric methods");
	}

	if (key->isEncryptedKey() && key->getIdWrapperKey() != idMasterKey)
	{
		throw CYDataException("This key encrypted other");
	}

	symmetricEncryptKey = key;
}

void YSecurity::CYSecurityFacad::setPublicEncryptKeyToSend(YSecurity::CYKey * key)
{
	if (!key)
	{
		throw CYDataException("It is not public key for enrypt methods");
	}

	if (key && key->getType() != YTypeKey::publicKeyEncrypt)
	{
		throw CYDataException("It is not public key for enrypt methods");
	}

	if (key->isEncryptedKey() && key->getIdWrapperKey() != idMasterKey)
	{
		throw CYDataException("This key encrypted other");
	}

	publicEncryptKeyToSend = key;
}

void YSecurity::CYSecurityFacad::setPrivateSignKeyToSend(YSecurity::CYKey * key)
{
	if (!key)
	{
		throw CYDataException("It is not private key for sign methods");
	}

	if (key && key->getType() != YTypeKey::privateKeySign)
	{
		throw CYDataException("It is not private key for sign methods");
	}

	if (key->isEncryptedKey() && key->getIdWrapperKey() != idMasterKey)
	{
		throw CYDataException("This key encrypted other");
	}

	privateSignKeyToSend = key;
}

void YSecurity::CYSecurityFacad::setPrivateEncryptKeyToReceive(YSecurity::CYKey * key)
{
	if (!key)
	{
		throw CYDataException("It is not private key for enrypt methods");
	}

	if (key && key->getType() != YTypeKey::privateKeyEncrypt)
	{
		throw CYDataException("It is not private key for enrypt methods");
	}

	if (key->isEncryptedKey() && key->getIdWrapperKey() != idMasterKey)
	{
		throw CYDataException("This key encrypted other");
	}
	
	privateEncryptKeyToReceive = key;
}

void YSecurity::CYSecurityFacad::setPublicSignKeyToReceive(YSecurity::CYKey * key)
{
	if (!key)
	{
		throw CYDataException("It is not public key for sign methods");
	}

	if (key && key->getType() != YTypeKey::publicKeySign)
	{
		throw CYDataException("It is not public key for sign methods");
	}

	if (key->isEncryptedKey() && key->getIdWrapperKey() != idMasterKey)
	{
		throw CYDataException("This key encrypted other");
	}

	publicSignKeyToReceive = key;
}

YSecurity::CYData<YSecurity::ubyte> YSecurity::CYSecurityFacad::getHashKey(YSecurity::CYKey * key, const char * hashName)
{
	if (!isInit)
		throw CYInitializationException();
	if (!password)
		throw CYPasswordException();
	if (!key)
		throw CYDataException("error key");

	if(key->isEncryptedKey())
	{
		auto masterKey = getMasterKey(key->getSalt());

		auto uPtrMasterKey = std::unique_ptr<CYKey>(masterKey);

		return key->getHashKey(hashName, masterKey);
	}
	else
	{
		return key->getHashKey(hashName, NULL);
	}
}


