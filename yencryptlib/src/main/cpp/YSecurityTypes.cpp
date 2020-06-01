#include "YSecurityTypes.h"
#include "YTime.h"
#include "WrapperSymmetricEncryptor.h"
#include "WrapperDigest.h"
#include <memory>
YSecurity::CYKey::CYKey(yUint ver, uint_64 id, 
	yChar * algorithm, yUint algorithmLen, 
	yChar * algorithmHash, yUint algorithmHashLen, 
	yByte * key, 
	yUint timeLife, yDate date, yUint lenBits, yUint lenCurBytes, 
	bool isEncrypted, YTypeKey type,
	yByte * salt, yUint saltLen):
	ver(ver), id(id), timeLife(timeLife), date(date), lenBits(lenBits), algorithmHash(nullptr), isEncrypted(isEncrypted), type(type),
	salt(nullptr)
{
	this->algorithm = new yString(algorithm, algorithmLen);
	if(algorithmHash)
		this->algorithmHash = new yString(algorithmHash, algorithmHashLen);
	if (salt)
		this->salt = new yBytes(salt, saltLen);
	this->key = new yBytes(key, lenCurBytes);
}

YSecurity::CYKey::CYKey(yByte * dataKey, yUint lenDataKey): algorithmHash(nullptr), salt(nullptr)
{
	//totalLen + version + meta + id + timeLife + dataCreate + algorithmLen + algorithm + keyLen + key + keyLenBits + (algorithHashLen + algorithHash) + (saltLen + salt)
	// 4       + 4       + 4    + 8  + 4        + 8          + 4            + ?         + 4      + ?   + 4          + (4               + ?           ) + (4       + ?   )
	//44 + 4 + 4
	yUint pos = 0;
	yUint totalLen = 0;

	memcpy(&totalLen, &dataKey[pos], sizeof(totalLen));
	if (totalLen != lenDataKey)
	{
		throw CYKeyException("Error format key. Length data is not equal totalLen field");
	}
	pos += sizeof(totalLen);

	memcpy(&ver, &dataKey[pos], sizeof(ver));
	pos += sizeof(ver);

	yUint meta = 0;
	memcpy(&meta, &dataKey[pos], sizeof(meta));
	pos += sizeof(meta);

	if (!(meta & 0x8000'0000))
	{
		throw CYKeyException("Error format key. It's not key");
	}

	isEncrypted = meta & 1;

	yUint typeKey = (meta >> 1) & 7;
	
	switch (typeKey)
	{
	case 0:
		type = YTypeKey::secretKey;
		break;
	case 1:
		type = YTypeKey::publicKeyEncrypt;
		break;
	case 2:
		type = YTypeKey::privateKeyEncrypt;
		break;
	case 3:
		type = YTypeKey::publicKeySign;
		break;
	case 4:
		type = YTypeKey::privateKeySign;
		break;
	default:
		throw CYKeyException("Error format key. Unknown type key");
		break;
	}

	bool hasSalt = (meta >> 5) & 1;

	memcpy(&id, &dataKey[pos], sizeof(id));
	pos += sizeof(id);
	
	memcpy(&timeLife, &dataKey[pos], sizeof(timeLife));
	pos += sizeof(timeLife);

	memcpy(&date, &dataKey[pos], sizeof(date));
	pos += sizeof(date);

	yUint algoritmLen = 0;
	memcpy(&algoritmLen, &dataKey[pos], sizeof(algoritmLen));
	pos += sizeof(algoritmLen);

	if (pos + algoritmLen + 4 + 1 + 4 > lenDataKey)
	{
		throw CYKeyException("Error format key. Algorithm");
	}

	this->algorithm = new yString((char_t *)&dataKey[pos], algoritmLen);
	pos += algoritmLen;

	yUint keyLen = 0;
	memcpy(&keyLen, &dataKey[pos], sizeof(keyLen));
	pos += sizeof(keyLen);

	if (pos + keyLen + 4 > lenDataKey)
	{
		throw CYKeyException("Error format key. Key");
	}

	this->key = new yBytes(&dataKey[pos], keyLen);
	pos += keyLen;

	memcpy(&lenBits, &dataKey[pos], sizeof(lenBits));
	pos += sizeof(lenBits);

	if (type != YTypeKey::secretKey)
	{
		if (pos + 1 + 4 > lenDataKey)
		{
			throw CYKeyException("Error format key. Hash is not define");
		}
		yUint lenHash = 0;
		memcpy(&lenHash, &dataKey[pos], sizeof(lenHash));
		pos += sizeof(lenHash);
		if (pos + lenHash > lenDataKey)
		{
			throw CYKeyException("Error format key. Hash name. Lenght is error");
		}
		this->algorithmHash = new yString((char_t*)&dataKey[pos], lenHash);
		pos += lenHash;
	}

	if (hasSalt)
	{
		if (pos + 1 + 4 > lenDataKey)
		{
			throw CYKeyException("Error format key. Salt is not define");
		}
		yUint lenSalt = 0;
		memcpy(&lenSalt, &dataKey[pos], sizeof(lenSalt));
		pos += sizeof(lenSalt);
		if (pos + lenSalt > lenDataKey)
		{
			throw CYKeyException("Error format key. Hash name. Lenght is error");
		}
		this->salt = new yBytes(&dataKey[pos], lenSalt);
		pos += lenSalt;
	}

	if (pos != lenDataKey)
	{
		throw CYKeyException("Error format key. Parser error");
	}
}

YSecurity::CYKey::~CYKey()
{
	delete algorithm;
	delete algorithmHash;
	delete key;
	delete salt;
}

bool YSecurity::CYKey::isAlive()
{
	return YTime::isAlive(date, timeLife);
}

bool YSecurity::CYKey::hasSalt()
{
	return salt!=nullptr;
}

YSecurity::yBytes * YSecurity::CYKey::getSalt()
{
	return salt;
}

YSecurity::yDate YSecurity::CYKey::getDate()
{
	return date;
}

YSecurity::yUint YSecurity::CYKey::getTimelife()
{
	return timeLife;
}

YSecurity::CYKey * YSecurity::CYKey::getEncryptedKey(CYKey * wrapperKey, CYKey * oldWrapperKey)
{
	
	WrapperSymmetricEncryptor encrypter;
	yBytes * keyData = nullptr;
	if (isEncrypted && oldWrapperKey != nullptr)
	{
		auto decryptedData = encrypter.decryptData(oldWrapperKey, key);
		keyData = encrypter.encryptData(wrapperKey, decryptedData);
		delete decryptedData;
	}
	else
	{
		keyData = encrypter.encryptData(wrapperKey, key);
	}

	auto encrypedKey = new CYKey(ver, id, 
		algorithm->getData(), algorithm->getLenData(), 
		algorithmHash ? algorithmHash->getData() : nullptr,	algorithmHash ? algorithmHash->getLenData() : 0,
		keyData->getData(), timeLife, date, lenBits, keyData->getLenData(), 
		true, type,
		wrapperKey->hasSalt()? wrapperKey->salt->getData():nullptr,
		wrapperKey->hasSalt()? wrapperKey->salt->getLenData() : 0);

	delete keyData;
	return encrypedKey;
}

YSecurity::CYKey * YSecurity::CYKey::getDecryptedKey(CYKey * wrapperKey)
{
	if (!isEncrypted)
		throw CYKeyException("Key is not encrypted");

	WrapperSymmetricEncryptor encrypter;
	yBytes * keyData = encrypter.decryptData(wrapperKey, key);

	auto decryptKey = new CYKey(ver, id, 
		algorithm->getData(), algorithm->getLenData(),
		algorithmHash ? algorithmHash->getData() : nullptr,	algorithmHash ? algorithmHash->getLenData() : 0,
		keyData->getData(), timeLife, date, lenBits, keyData->getLenData(),
		false, type,
		nullptr, 0);
	delete keyData;
	return decryptKey;
	//return 0;
}

YSecurity::yBytes * YSecurity::CYKey::getBytesDecryptedKeyData(CYKey * wrapperKey)
{
	auto decryptedKey = getDecryptedKey(wrapperKey);

	auto decryptedData = getBytesKeyData(decryptedKey->key);

	delete decryptedKey;

	return decryptedData;
	//return nullptr;
}

YSecurity::yBytes * YSecurity::CYKey::getBytesKeyData()
{
	return getBytesKeyData(key);
}

YSecurity::yBytes YSecurity::CYKey::getHashKey(const char * hashName, CYKey * wrapperKey)
{
	if (isEncrypted && wrapperKey!=nullptr)
	{
		auto decryptedKey = getDecryptedKey(wrapperKey);
		auto uPtrKey = std::unique_ptr<CYKey>(decryptedKey);
		return decryptedKey->getHashKey(hashName);
	}
	WrapperDigest digest((char*)hashName);
	return digest.getHash(*key);
}

YSecurity::yBigUint YSecurity::CYKey::getIdWrapperKey()
{
	if (!isEncrypted)
		throw CYKeyException("Key is not encrypted");
	yBigUint wrapperId = 0;
	memcpy(&wrapperId, this->key->getData(), sizeof(wrapperId));
	return wrapperId;
}

YSecurity::yBytes * YSecurity::CYKey::getKeyBytes()
{
	return key;
}

YSecurity::yBigUint YSecurity::CYKey::getId()
{
	return id;
}

YSecurity::YTypeKey YSecurity::CYKey::getType()
{
	return type;
}

bool YSecurity::CYKey::isEncryptedKey()
{
	return isEncrypted;
}

const char * YSecurity::CYKey::getHashName()
{
	if (this->algorithmHash)
	{
		if (strncmp(algorithmHash->getData(), SHA_256_ALGO, algorithmHash->getLenData())==0)
		{
			return SHA_256;
		}
		else if (strncmp(algorithmHash->getData(), SHA_384_ALGO, algorithmHash->getLenData())==0)
		{
			return SHA_384;
		}
		else if (strncmp(algorithmHash->getData(), SHA_512_ALGO, algorithmHash->getLenData())==0)
		{
			return SHA_512;
		}
	}
	
	return SHA_256;
}

YSecurity::yBytes * YSecurity::CYKey::getBytesKeyData(yBytes * key)
{
	yUint totalLen = 4 * sizeof(yUint) +
		sizeof(ver) + sizeof(id) + sizeof(timeLife) + sizeof(date) + sizeof(lenBits) +
		algorithm->getLenData() + key->getLenData();

	if (type != YTypeKey::secretKey)
	{
		totalLen += sizeof(yUint) + algorithmHash->getLenData();
	}

	if (salt != nullptr)
	{
		totalLen += sizeof(yUint) + salt->getLenData();
	}

	yBytes * data = new yBytes(totalLen);

	yUint pos = 0;
	//totalLen + version + meta + id + timeLife + dataCreate +
	//algorithmLen + algorithm + keyLen + key + keyLenBits + (algorithHashLen + algorithHash) + (saltLen + salt)

	data->past(pos, (ubyte*)&totalLen, sizeof(totalLen));
	pos += sizeof(totalLen);
	data->past(pos, (ubyte*)&ver, sizeof(ver));
	pos += sizeof(ver);

	yUint meta = 0;

	switch (type)
	{
	case YTypeKey::secretKey:
		meta = 0;
		break;
	case YTypeKey::publicKeyEncrypt:
		meta = 1;
		break;
	case YTypeKey::privateKeyEncrypt:
		meta = 2;
		break;
	case YTypeKey::publicKeySign:
		meta = 3;
		break;
	case YTypeKey::privateKeySign:
		meta = 4;
		break;
	default:
		break;
	}

	meta = meta << 1;

	if (isEncrypted)
		meta = meta | 1;

	meta = meta | 0x8000'0000;
	if (salt)
	{
		meta = meta | 32;
	}

	data->past(pos, (ubyte*)&meta, sizeof(meta));
	pos += sizeof(meta);

	data->past(pos, (ubyte*)&id, sizeof(id));
	pos += sizeof(id);
	
	data->past(pos, (ubyte*)&timeLife, sizeof(timeLife));
	pos += sizeof(timeLife);

	data->past(pos, (ubyte*)&date, sizeof(date));
	pos += sizeof(date);

	data->past(pos, (ubyte*)&algorithm->getLenData(), sizeof(yUint));
	pos += sizeof(yUint);

	data->past(pos, (ubyte*)algorithm->getData(), algorithm->getLenData());
	pos += algorithm->getLenData();

	data->past(pos, (ubyte*)&key->getLenData(), sizeof(yUint));
	pos += sizeof(yUint);

	data->past(pos, (ubyte*)key->getData(), key->getLenData());
	pos += key->getLenData();

	data->past(pos, (ubyte*)&lenBits, sizeof(lenBits));
	pos += sizeof(lenBits);

	if (type != YTypeKey::secretKey)
	{
		data->past(pos, (ubyte*)&algorithmHash->getLenData(), sizeof(yUint));
		pos += sizeof(yUint);

		data->past(pos, (ubyte*)algorithmHash->getData(), algorithmHash->getLenData());
		pos += algorithmHash->getLenData();
	}
	if (salt)
	{
		data->past(pos, (ubyte*)&salt->getLenData(), sizeof(yUint));
		pos += sizeof(yUint);

		data->past(pos, (ubyte*)salt->getData(), salt->getLenData());
		pos += salt->getLenData();
	}
	if(totalLen!=pos)
	{
		throw CYException("Pack key info");
	}

	return data;
}

YSecurity::yBytes * YSecurity::CYKey::getBytesEncryptedKeyData(CYKey * wrapperKey, CYKey * oldWrapperKey)
{
	auto keyEncrypted = getEncryptedKey(wrapperKey, oldWrapperKey);

	auto bytesEncrypted = getBytesKeyData(keyEncrypted->key);

	delete keyEncrypted;

	return bytesEncrypted;
}

YSecurity::CYMsg::CYMsg(yBytes & data): CYMsgInfo(data), data(0), signature(0), pars(true)
{
	yUint const offset = sizeof(yUint) + sizeof(ver) + sizeof(meta) + sizeof(type) + sizeof(idKey) + sizeof(dateCreate) + sizeof(timeLife);
	yUint totalLen = data.getLenData();
	yUint pos = offset;
	
	yUint dataLen;
	data.copyBytes(pos, &dataLen, sizeof(dataLen));
	pos += sizeof(dataLen);

	this->data = new yBytes(dataLen);
	data.copyBytes(pos, this->data->getData(), dataLen);
	pos += dataLen;

	
	if (meta & 2)
	{		
		yUint signatureLen = totalLen - pos;
		this->signature = new yBytes(signatureLen);
		data.copyBytes(pos, this->signature->getData(), signatureLen);
		pos += signatureLen;
	}

	if (pos != totalLen)
	{
		throw CYDataException("Error format msg");
	}
}

YSecurity::CYMsg::CYMsg(YSecurity::uint_32 version, YSecurity::uint_32 type,
	YSecurity::uint_32 meta, YSecurity::uint_32 timeLife, 
	YSecurity::date dateCreate, YSecurity::uint_64 idKey, 
	YSecurity::CYData<YSecurity::ubyte>* data, YSecurity::CYData<YSecurity::ubyte>* signature): pars(false)
{
	ver = version;
	this->type = type;
	this->meta = meta;
	this->timeLife = timeLife;
	this->dateCreate = dateCreate;
	this->idKey = idKey;
	this->data = data;
	this->signature = signature;
}

YSecurity::CYMsg::~CYMsg()
{
	if (pars)
	{
		if (data)
			delete data;
		if (signature)
			delete signature;
	}
}

YSecurity::yBytes * YSecurity::CYMsg::getData()
{
	return data;
}

YSecurity::yBytes * YSecurity::CYMsg::getSignature()
{
	return signature;
}

YSecurity::yBytes YSecurity::CYMsg::getBytes()
{
	yUint totalSize = sizeof(yUint) * 2 + sizeof(ver) + sizeof(type) + sizeof(meta) + sizeof(timeLife) + sizeof(dateCreate) +
		sizeof(idKey) + data->getLenData();

	if (signature)
	{
		totalSize += signature->getLenData();
	}

	yBytes msg(totalSize);
	yUint pos = 0;

	msg.past(pos, &totalSize, sizeof(totalSize));
	pos += sizeof(totalSize);
	msg.past(pos, &ver, sizeof(ver));
	pos += sizeof(ver);
	msg.past(pos, &meta, sizeof(meta));
	pos += sizeof(meta);
	msg.past(pos, &type, sizeof(type));
	pos += sizeof(type);
	msg.past(pos, &timeLife, sizeof(timeLife));
	pos += sizeof(timeLife);
	msg.past(pos, &dateCreate, sizeof(dateCreate));
	pos += sizeof(dateCreate);
	msg.past(pos, &idKey, sizeof(idKey));
	pos += sizeof(idKey);

	yUint len = data->getLenData();
	msg.past(pos, &len, sizeof(len));
	pos += sizeof(len);

	msg.past(pos, data->getData(), len);
	pos += len;

	if (signature)
	{
		msg.past(pos, signature->getData(), signature->getLenData());
		pos += signature->getLenData();
	}

	if (pos != totalSize)
	{
		throw CYException("error pack msg");
	}
	return msg;

}

YSecurity::CYMsgInfo::CYMsgInfo(yBytes & data)
{
	yUint totalLen = 0;
	yUint pos = 0;
	data.copyBytes(pos, &totalLen, sizeof(totalLen));
	pos += sizeof(totalLen);

	if (data.getLenData() != totalLen)
	{
		throw CYException("Unpack msg");
	}

	data.copyBytes(pos, &ver, sizeof(ver));
	pos += sizeof(ver);

	data.copyBytes(pos, &meta, sizeof(meta));
	pos += sizeof(meta);

	data.copyBytes(pos, &type, sizeof(type));
	pos += sizeof(type);

	data.copyBytes(pos, &timeLife, sizeof(timeLife));
	pos += sizeof(timeLife);

	data.copyBytes(pos, &dateCreate, sizeof(dateCreate));
	pos += sizeof(dateCreate);

	data.copyBytes(pos, &idKey, sizeof(idKey));
	pos += sizeof(idKey);

}

YSecurity::CYMsgInfo::CYMsgInfo()
{
}

YSecurity::yUint YSecurity::CYMsgInfo::getTimeLife()
{
	return timeLife;
}

YSecurity::yDate YSecurity::CYMsgInfo::getDateCreate()
{
	return dateCreate;
}

YSecurity::yUint YSecurity::CYMsgInfo::getVer()
{
	return ver;
}

YSecurity::yUint YSecurity::CYMsgInfo::getMeta()
{
	return meta;
}

YSecurity::yUint YSecurity::CYMsgInfo::getType()
{
	return type;
}

YSecurity::yBigUint YSecurity::CYMsgInfo::getIdKey()
{
	return idKey;
}

bool YSecurity::CYMsgInfo::isEncryped()
{
	return meta & 1;
}

bool YSecurity::CYMsgInfo::isSigned()
{
	return meta & 2;
}

bool YSecurity::CYMsgInfo::isAlive()
{
	return YSecurity::YTime::isAlive(dateCreate, timeLife);
}

YSecurity::CYMetaMsg::CYMetaMsg(yBytes & data) :
	CYMsgInfo(data), attach(0)
{
	if (meta == 3)
	{
		const yUint offset = sizeof(yUint) + sizeof(ver) + sizeof(meta) + sizeof(type) + sizeof(idKey) + sizeof(dateCreate) + sizeof(timeLife);
		yUint pos = offset;
		yUint dataLen;
		data.copyBytes(pos, &dataLen, sizeof(dataLen));
		pos += sizeof(yUint);
		attach = new CYMetaMsg(data.copy(pos, pos + dataLen));
	}
}

YSecurity::CYMetaMsg::CYMetaMsg(yBytes && data):
	CYMsgInfo(data), attach(0)
{
	if (meta == 3)
	{
		const yUint offset = sizeof(yUint) + sizeof(ver) + sizeof(meta) + sizeof(type) + sizeof(idKey) + sizeof(dateCreate) + sizeof(timeLife);
		yUint pos = offset;
		yUint dataLen;
		data.copyBytes(pos, &dataLen, sizeof(dataLen));
		pos += sizeof(yUint);
		attach = new CYMetaMsg(data.copy(pos, pos + dataLen));
	}
}

YSecurity::CYMetaMsg * YSecurity::CYMetaMsg::getAttach()
{
	return attach;
}

YSecurity::CYMetaMsg::~CYMetaMsg()
{
	if (attach)
	{
		delete attach;
	}
}
