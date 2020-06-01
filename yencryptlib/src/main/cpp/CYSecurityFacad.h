#pragma once
#include "YSecurityTypes.h"

namespace YSecurity
{
	class CYSecurityFacad
	{
		YSecurity::uint_32 minRandomLen;
		YSecurity::uint_32 maxRandomLen;

		YSecurity::CYData<YSecurity::char_t> * password;

		YSecurity::CYKey * symmetricEncryptKey;
		
		YSecurity::CYKey * publicEncryptKeyToSend;
		YSecurity::CYKey * privateSignKeyToSend;
		
		YSecurity::CYKey * privateEncryptKeyToReceive;
		YSecurity::CYKey * publicSignKeyToReceive;

		YSecurity::uint_64 idMasterKey;

		static bool isInit;

	private:

		YSecurity::CYKey * getMasterKey(YSecurity::CYData<YSecurity::ubyte> * salt = NULL);

		YSecurity::CYKeysPair * getAsymmetricKeys(YSecurity::uint_32 lenKey, YSecurity::uint_32 version,
			YSecurity::uint_64 id, YSecurity::uint_32 timeLife, bool isSign, const char * hash);

		void setEmptyKeys();
		YSecurity::CYData<YSecurity::ubyte> randomPaddingByte(YSecurity::uint_32 sizeData);
				
		YSecurity::CYData<YSecurity::ubyte> encryptDataSymmetricKey(YSecurity::CYData<YSecurity::ubyte> & data, YSecurity::CYKey * symmetricKey);
		YSecurity::CYData<YSecurity::ubyte> encryptDataSymmetricKey(YSecurity::CYData<YSecurity::ubyte> & data, YSecurity::CYData<YSecurity::ubyte> & symmetricKey);

		YSecurity::CYData<YSecurity::ubyte> encryptDataAsymmetricKey(YSecurity::CYData<YSecurity::ubyte> & data);
		YSecurity::CYData<YSecurity::ubyte> decryptDataAsymmetricKey(YSecurity::CYData<YSecurity::ubyte> & data);

		YSecurity::CYData<YSecurity::ubyte> packData(YSecurity::CYData<ubyte> & data);
		YSecurity::CYData<YSecurity::ubyte> unpackData(YSecurity::CYData<YSecurity::ubyte> & data);
		
		YSecurity::CYData<YSecurity::ubyte> signDate(YSecurity::CYData<YSecurity::ubyte> & data);
		bool verifyData(YSecurity::CYData<YSecurity::ubyte> & data, YSecurity::CYData<YSecurity::ubyte> & signature);

		YSecurity::CYData<YSecurity::ubyte> packMsg(
			YSecurity::uint_32 version, YSecurity::uint_32 type, YSecurity::uint_32 meta, 
			YSecurity::uint_32 timeLife, YSecurity::date dateCreate,
			YSecurity::uint_64 idKey, YSecurity::CYData<YSecurity::ubyte> * data, 
			YSecurity::CYData<YSecurity::ubyte> * signature);

		YSecurity::CYMsg * unpackMsg(YSecurity::CYData<YSecurity::ubyte> & data);

	public:
		CYSecurityFacad();
		CYSecurityFacad(YSecurity::CYData<YSecurity::char_t> & password, YSecurity::uint_64 idMasterKey = 1);
		~CYSecurityFacad();

		void clear();

		static YSecurity::CYKey * generateSymmetricKeyFromPassword(YSecurity::CYData<YSecurity::char_t> * password,
				YSecurity::CYData<YSecurity::ubyte> * salt, yBigUint idKey);

		static YSecurity::CYData<YSecurity::ubyte> * encryptData(YSecurity::uint_32 version,
				YSecurity::CYData<YSecurity::ubyte> & data, YSecurity::CYKey * key);

		static YSecurity::CYData<YSecurity::ubyte> * decryptData(YSecurity::uint_32 version,
						YSecurity::CYData<YSecurity::ubyte> & data, YSecurity::CYKey * key);

		void setPassword(YSecurity::CYData<YSecurity::char_t> & password);

		void setRandomLen(YSecurity::uint_32 min, YSecurity::uint_32 max);

		YSecurity::CYKey * getSymmetricKey(YSecurity::uint_32 version, YSecurity::uint_64 id, YSecurity::uint_32 timeLife);

		YSecurity::CYKeysPair * getShortAsymmetricKeys(YSecurity::uint_32 version, YSecurity::uint_64 id, YSecurity::uint_32 timeLife, bool isSign = true, const char * hash=SHA_256_ALGO);
		YSecurity::CYKeysPair * getMidleAsymmetricKeys(YSecurity::uint_32 version, YSecurity::uint_64 id, YSecurity::uint_32 timeLife, bool isSign = true, const char * hash = SHA_512_ALGO);
		YSecurity::CYKeysPair * getLongAsymmetricKeys(YSecurity::uint_32 version,  YSecurity::uint_64 id,  YSecurity::uint_32 timeLife, bool isSign = true,const char *  hash = SHA_512_ALGO);

		YSecurity::CYData<YSecurity::ubyte> encryptSecretMsg(YSecurity::uint_32 version, YSecurity::uint_32 type, 
			YSecurity::uint_32 timeLife, YSecurity::CYData<YSecurity::ubyte> & secretMsg, YSecurity::uint_32 meta=0);
		YSecurity::CYData<YSecurity::ubyte> encrypKeysMsg(YSecurity::uint_32 version, YSecurity::uint_32 timeLife, YSecurity::CYKey * keys,
			YSecurity::uint_32 type=0, YSecurity::uint_32 meta=0);
		YSecurity::CYData<YSecurity::ubyte> signSecretMsg(YSecurity::uint_32 version, YSecurity::uint_32 type,
			YSecurity::uint_32 timeLife, YSecurity::CYData<YSecurity::ubyte> & secretMsg, YSecurity::uint_32 meta = 0);

		YSecurity::CYSecretMsg * decryptSecretMsg(YSecurity::CYData<YSecurity::ubyte> & secretMsg);
		YSecurity::CYKey * decrypKeysMsg(YSecurity::CYData<YSecurity::ubyte> & keyMsg);
		YSecurity::CYSecretMsg * veryfiSecretMsg(YSecurity::CYData<YSecurity::ubyte> & secretMsg);

		void setSymmetricEncryptKey(YSecurity::CYKey * key);

		void setPublicEncryptKeyToSend(YSecurity::CYKey * key);
		void setPrivateSignKeyToSend(YSecurity::CYKey * key);

		void setPrivateEncryptKeyToReceive(YSecurity::CYKey * key);
		void setPublicSignKeyToReceive(YSecurity::CYKey * key);

		YSecurity::CYData<YSecurity::ubyte> getHashKey(YSecurity::CYKey * key, const char * hashName = SHA_512);

		YSecurity::CYData<YSecurity::ubyte> changeMasterPassword(YSecurity::CYKey * key, YSecurity::CYData<YSecurity::char_t> & password, YSecurity::uint_64 idMasterKey = 1);

		static YSecurity::CYMetaMsg * getMetaData(YSecurity::CYData<YSecurity::ubyte> & msg);
		static void init(YSecurity::CYData<YSecurity::ubyte> seed);
		static void addRandom(YSecurity::CYData<YSecurity::ubyte> seed);
	};
}

