#pragma once
#include <cstring>
#include "YSecurityElementaryTypes.h"
#include "CYException.h"


#define AES_256_CBC "AES-256-CBC"
#define SYMMETRIC_ALGORITHM AES_256_CBC
#define SHA_512 "SHA512"
#define SHA_384 "SHA384"
#define SHA_256 "SHA256"

#define RSA_OAEP_ALGO "RSA-OAEP"
#define RSA_PSS_ALGO "RSA-PSS"

#define AES_CBC_ALGO "AES-CBC"
#define SHA_512_ALGO "SHA-512"
#define SHA_384_ALGO "SHA-384"
#define SHA_256_ALGO "SHA-256"
#define PBKDF2_ALGO "PBKDF2"
namespace YSecurity
{
	template <class T> class CYData
	{
		YSecurity::uint_32 lenData;
		T * data;

		//CYData() : lenData(0), data(nullptr) {}
	public:	
		static CYData<T> * createPtr(T * data, YSecurity::uint_32 lenData)
		{
			CYData<T> * dataPtr = new CYData();

			dataPtr->data = data;
			dataPtr->lenData = lenData;

			return dataPtr;
		}

		CYData(): lenData(0), data(nullptr)
		{
		}

		CYData(YSecurity::uint_32 lenData)
		{
			this->lenData = lenData;
			this->data = new T[lenData];
		}

		CYData(T * data, YSecurity::uint_32 lenData)
		{
			this->lenData = lenData;
			this->data = new T[lenData];
			for (YSecurity::uint_32 i=0; i< lenData; i++)
			{
				this->data[i] = data[i];
			}
		}

		CYData(YSecurity::uint_32 lenData, YSecurity::ubyte * data)
		{
			this->lenData = lenData/sizeof(T);
			this->data = (T*) new ubyte[lenData];
			memcpy(this->data, data, lenData);
		}

		CYData(CYData<T> & data)
		{
			this->lenData = data.lenData;
			this->data = new T[lenData];
			memcpy(this->data, data.data, lenData * sizeof(T));
		}

		CYData(CYData<T> && data)
		{
			this->lenData = data.lenData;
			this->data = data.data;
			data.data = nullptr;
		}

		T * at(YSecurity::uint_32 index)
		{
			return &this->data[index];
		}

		void past(YSecurity::uint_32 pos, void * data, YSecurity::uint_32 len)
		{
			memcpy(&this->data[pos], data, len * sizeof(T));
		}

		void copyBytes(YSecurity::uint_32 pos, void * data, YSecurity::uint_32 len)
		{
			memcpy(data, &this->data[pos], len);
		}

		void past(YSecurity::uint_32 pos, CYData<T> & data)
		{
			past(pos, data.data, data.lenData);
		}

		CYData<T> copy(YSecurity::uint_32 start=0, YSecurity::uint_32 end=0)
		{
			if (end == 0)
				end = lenData;
			YSecurity::uint_32 len = end - start;
			return CYData<T>(&data[start], len);
		}

		void add(CYData<T> * data)
		{
			add(data->data, data->lenData);
		}

		void add(CYData<T> & data)
		{
			add(data.data, data.lenData);
		}

		void add(T * addData, YSecurity::uint_32 addLen)
		{
			YSecurity::uint_32 newLen = addLen;
			if (lenData>0)
			{
				newLen += lenData;
			}
			auto t = data;
			data = new T[newLen];
			if (t)
			{
				for (YSecurity::uint_32 i = 0; i < lenData; i++)
				{
					data[i] = t[i];
				}
				delete[] t;
			}
			for (YSecurity::uint_32 i = 0; i < addLen; i++)
			{
				data[lenData + i] = addData[i];
			}
			lenData = newLen;
		}


		CYData<T> & operator = (CYData<T> & data)
		{
			this->lenData = data.lenData;
			this->data = new T[lenData];
			memcpy(this->data, data.data, lenData * sizeof(T));
			return *this;
		}

		CYData<T> & operator = (CYData<T> && data)
		{
			this->lenData = data.lenData;
			this->data = data.data;
			data.data = nullptr;
			return *this;
		}


		~CYData()
		{
			if(this->data)
			{
				memset(this->data, 0, this->lenData*sizeof(T));
				delete[] this->data;
				this->data = 0;
				this->lenData = 0;
			}
		}

		YSecurity::uint_32 & getLenData()
		{
			return this->lenData;
		}

		T * getData()
		{
			return this->data;
		}

		T * operator [] (YSecurity::uint_32 n)
		{
			return &this->data[n];
		}

		uint_32 getSizeInBytes()
		{
			return sizeof(T) * this->lenData;
		}
		
		uint_32 getSizeItem()
		{
			return sizeof(T);
		}

		static uint_32 getSizeLenData()
		{
			return sizeof(lenData);
		}
	};

	typedef YSecurity::CYData<YSecurity::ubyte> yBytes;
	typedef YSecurity::CYData<YSecurity::char_t> yString;

	typedef YSecurity::uint_32 yUint;
	typedef YSecurity::uint_64 yBigUint;
	typedef YSecurity::date yDate;
	typedef YSecurity::char_t yChar;
	typedef YSecurity::ubyte yByte;

	enum class YTypeKey
	{
		secretKey,
		publicKeyEncrypt,
		privateKeyEncrypt,
		publicKeySign,
		privateKeySign
	};

	class CYKey
	{
		yUint ver;
		yBigUint id;
		yString  * algorithm;
		yString * algorithmHash;
		yBytes * key;
		yBytes * salt;
		yUint timeLife;
		yDate date;
		yUint lenBits;
		bool isEncrypted;
		YTypeKey type;
	public:
		CYKey(yUint ver, uint_64 id,
			yChar * algorithm, yUint algorithmLen, 
			yChar * algorithmHash, yUint algorithmHashLen,
			yByte * key, 
			yUint timeLife, yDate date, yUint lenBits, yUint lenCurBytes, 
			bool isEncrypted, YTypeKey type, 
			yByte * salt, yUint saltLen);

		CYKey(yByte * dataKey, yUint lenDataKey);
		~CYKey();

		bool isAlive();
		bool hasSalt();
		yBytes * getSalt();
		yDate getDate();
		yUint getTimelife();
		CYKey * getEncryptedKey(CYKey * wrapperKey, CYKey * oldWrapperKey = nullptr);
		CYKey * getDecryptedKey(CYKey * wrapperKey);
		yBytes * getBytesEncryptedKeyData(CYKey * wrapperKey=nullptr, CYKey * oldWrapperKey = nullptr);
		yBytes * getBytesDecryptedKeyData(CYKey * wrapperKey = nullptr);
		yBytes * getBytesKeyData();
		yBytes getHashKey(const char * hashName = SHA_512, CYKey * wrapperKey = nullptr);
		yBigUint getIdWrapperKey();
		yBytes * getKeyBytes();
		yBigUint getId();
		YTypeKey getType();
		bool isEncryptedKey();
		const char * getHashName();
	private:
		yBytes * getBytesKeyData(yBytes * key);
	};

	class CYMsgInfo
	{
	protected:
		yUint ver;
		yUint meta;
		yUint type;
		yBigUint idKey;
		yUint timeLife;
		yDate dateCreate;
	public:
		CYMsgInfo(yBytes & data);
		//CYMsgInfo(yBytes && data);
		CYMsgInfo();
		yUint getTimeLife();
		yDate getDateCreate();
		yUint getVer();
		yUint getMeta();
		yUint getType();
		yBigUint getIdKey();
		bool isEncryped();
		bool isSigned();
		bool isAlive();
	};

	class CYMetaMsg : public CYMsgInfo
	{
		CYMetaMsg * attach;
	public:
		CYMetaMsg(yBytes & data);
		CYMetaMsg(yBytes && data);
		CYMetaMsg * getAttach();
		~CYMetaMsg();
	};

	class CYMsg : public CYMsgInfo
	{
		yBytes *data;
		yBytes * signature;	
		bool pars;
	public:
		CYMsg(yBytes & data);
		CYMsg(YSecurity::uint_32 version, YSecurity::uint_32 type, YSecurity::uint_32 meta, 
			YSecurity::uint_32 timeLife, YSecurity::date dateCreate,
			YSecurity::uint_64 idKey, YSecurity::CYData<YSecurity::ubyte>* data, 
			YSecurity::CYData<YSecurity::ubyte>* signature);
		~CYMsg();
		yBytes * getData();
		yBytes * getSignature();
		yBytes getBytes();
	};

	class CYSecretMsg
	{
		yBytes data;
		yUint type;
		yUint ver;
		yUint meta;
	public:
		CYSecretMsg() {}
		CYSecretMsg(yBytes & data, yUint ver, yUint type, yUint meta): 
			data(data), ver(ver), type(type), meta(meta){}

		YSecurity::CYData<YSecurity::ubyte> getData() { return data; }
		YSecurity::uint_32 getType()
		{
			return type;
		}
		YSecurity::uint_32 getVer()
		{
			return ver;
		}
		YSecurity::uint_32 getMeta()
		{
			return meta;
		}
		~CYSecretMsg() {}
	};

	class CYKeysPair
	{
		CYKey * privateKey;
		CYKey * publicKey;
	public:
		CYKeysPair(CYKey * privateKey, CYKey * publicKey)
		{
			this->privateKey = privateKey;
			this->publicKey = publicKey;
		}
		CYKey * getPrivateKey()
		{
			return privateKey;
		}

		CYKey * getPublicKey()
		{
			return publicKey;
		}

		~CYKeysPair()
		{
			if (privateKey)
				delete privateKey;
			if (publicKey)
				delete publicKey;
		}
	};
}
