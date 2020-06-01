#include "COpenSSLRSADecryptor.h"

namespace YSecurity
{

	void COpenSSLRSADecryptor::getKeyFromBytes(unsigned char * key, int keyLen)
	{
		if (ctx != NULL)
		{
			EVP_PKEY_CTX_free(ctx);
		}

		if (this->key)
			EVP_PKEY_free(this->key);

		if (key == NULL || keyLen <= 0)
		{
			this->_isError = true;
			this->codeError = 1001;
			this->lastError = "Bed key";
			return;
		}

		rsa_st * rsa;
		const unsigned char * p = key;

		this->key = d2i_AutoPrivateKey(0, &p, keyLen);
		
		if (!this->key)
		{
			this->_isError = true;
			this->codeError = 1202;
			this->lastError = "Error pkey create";
			return;
		}
		rsa = EVP_PKEY_get1_RSA(this->key);

		if (!rsa)
		{
			this->_isError = true;
			this->codeError = 1201;
			this->lastError = "Error rsa key";
			return;
		}

		ctx = EVP_PKEY_CTX_new(this->key, NULL);

		if (!ctx)
		{
			this->_isError = true;
			this->codeError = 1301;
			this->lastError = "Error create context";
			return;
		}

		if (EVP_PKEY_decrypt_init(ctx) <= 0)
		{
			this->_isError = true;
			this->codeError = 1302;
			this->lastError = "Error init context";
			return;
		}

		if (EVP_PKEY_CTX_set_rsa_padding(ctx, RSA_PKCS1_OAEP_PADDING) <= 0)
		{
			this->_isError = true;
			this->codeError = 1401;
			this->lastError = "Error set padding";
			return;
		}

		auto md = EVP_get_digestbyname(mdName);
		if (md == nullptr)
		{
			this->_isError = true;
			this->codeError = 1409;
			this->lastError = "Error md name";
			return;
		}

		if (EVP_PKEY_CTX_set_rsa_oaep_md(ctx, md) <= 0)
		{
			this->_isError = true;
			this->codeError = 1402;
			this->lastError = "Error set hash";
			return;
		}
	}


	COpenSSLRSADecryptor::~COpenSSLRSADecryptor()
	{
		if (ctx != NULL)
		{
			EVP_PKEY_CTX_free(ctx);
			//OPENSSL_free(ctx);
		}

		if (key != NULL)
		{
			EVP_PKEY_free(key);
			//OPENSSL_free(key);
		}

		if (outData != NULL)
		{
			delete[] outData;
			outData = NULL;
		}
	}

	COpenSSLRSADecryptor::COpenSSLRSADecryptor(unsigned char * key, int keyLen, const char * mdName): mdName(mdName)
	{
		this->ctx = NULL;
		this->key = NULL;
		this->outData=NULL;
		this->lastError = "";
		this->_isError = false;
		this->codeError = 0;
		this->outData = NULL;
		this->lenOutData = 0;
		getKeyFromBytes(key, keyLen);
	}

	void COpenSSLRSADecryptor::setAsymmetricKey(unsigned char * key, int lenKey)
	{
		getKeyFromBytes(key, lenKey);
	}

	void COpenSSLRSADecryptor::decryptData(unsigned char * data, int lenData)
	{
		if (this->outData)
		{
			delete[] this->outData;
			this->lenOutData = 0;
		}

		if (EVP_PKEY_decrypt(ctx, NULL, &this->lenOutData, data, lenData) <= 0)
		{
			this->_isError = true;
			this->codeError = 2001;
			this->lastError = "Error get len out data";
			return;
		}

		this->outData = new unsigned char[this->lenOutData + 1];

		if (!this->outData)
		{
			this->_isError = true;
			this->codeError = 2102;
			this->lastError = "Error memory alloc";
			return;
		}

		if (EVP_PKEY_decrypt(ctx, this->outData, &this->lenOutData, data, lenData) <= 0)
		{
			this->_isError = true;
			this->codeError = 2002;
			this->lastError = "Error encrypt data";
			return;
		}
	}
	size_t COpenSSLRSADecryptor::getSizeBlock()
	{
		size_t size;
		if (EVP_PKEY_decrypt(ctx, NULL, &size, 0, 0) <= 0)
		{
			this->_isError = true;
			this->codeError = 2001;
			this->lastError = "Error get len out data";
			return 0;
		}

		return size;
	}
}
