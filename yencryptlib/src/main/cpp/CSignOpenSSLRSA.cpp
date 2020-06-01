#include "CSignOpenSSLRSA.h"
#include <openssl/rsa.h>
#include <openssl/x509.h>
#include <cmath>
namespace YSecurity
{
	void CSignOpenSSLRSA::setKeyFromBytes(unsigned char * key, size_t keyLen)
	{
		if (mdctx)
			EVP_MD_CTX_free(mdctx);
		if (pkey)
			EVP_PKEY_free(pkey);

		if (!(mdctx = EVP_MD_CTX_create()))
		{
			this->_isError = true;
			this->codeError = 1101;
			this->lastError = "Error create context md";
		}

		if (key == NULL || keyLen <= 0)
		{
			this->_isError = true;
			this->codeError = 1001;
			this->lastError = "Bed key";
			return;
		}

		rsa_st * rsa;
		const unsigned char * p = key;
		
		this->pkey = d2i_AutoPrivateKey(0, &p, keyLen);

		//auto pkcs8 = EVP_PKEY2PKCS8(pkey);
		//unsigned char * rawKey = nullptr;
		//int l = i2d_PKCS8_PRIV_KEY_INFO(pkcs8, &rawKey);

		if (this->pkey==nullptr)
		{
			this->_isError = true;
			this->codeError = 1202;
			this->lastError = "Error pkey";
			return;
		}
		rsa = EVP_PKEY_get1_RSA(this->pkey);

		if (!rsa)
		{
			this->_isError = true;
			this->codeError = 1201;
			this->lastError = "Error rsa key";
			return;
		}
		
		auto md = EVP_get_digestbyname(mdName);

		if (1 != EVP_DigestSignInit(mdctx, NULL, md, NULL, pkey))
		{
			this->_isError = true;
			this->codeError = 1301;
			this->lastError = "Error Init context Sign";
		}

		auto ctx = EVP_MD_CTX_pkey_ctx(mdctx);

		if (EVP_PKEY_CTX_set_rsa_padding(ctx, RSA_PKCS1_PSS_PADDING) <= 0)
		{
			this->_isError = true;
			this->codeError = 1401;
			this->lastError = "Error set padding";
			return;
		}

		if (EVP_PKEY_CTX_set_rsa_mgf1_md(ctx, md) <= 0)
		{
			this->_isError = true;
			this->codeError = 1402;
			this->lastError = "Error set hash";
			return;
		}
		//RSA_size(rsa);
		int lenSalt = ceil((RSA_size(rsa)*8 - 1)/8.0) - EVP_MD_size(md) - 2;

		if (EVP_PKEY_CTX_set_rsa_pss_saltlen(ctx, lenSalt) <=0)
		{
			this->_isError = true;
			this->codeError = 1403;
			this->lastError = "Error set len salt";
			return;
		}
	}

	CSignOpenSSLRSA::CSignOpenSSLRSA(unsigned char * key, size_t keyLen, const char * mdName)
	{
		this->mdName = mdName;
		this->lastError = "";
		this->_isError = false;
		this->codeError = 0;
		this->sign = NULL;
		this->lenSign = 0;
		mdctx=NULL;
		pkey=NULL;
		setKeyFromBytes(key, keyLen);
	}

	CSignOpenSSLRSA::~CSignOpenSSLRSA()
	{
		if (mdctx)
			EVP_MD_CTX_free(mdctx);
		if (pkey)
			EVP_PKEY_free(pkey);

	}

	void CSignOpenSSLRSA::addMsg(unsigned char * msg, size_t lenMsg)
	{
		if (1 != EVP_DigestSignUpdate(mdctx, (char*)msg, lenMsg))
		{
			this->_isError = true;
			this->codeError = 2001;
			this->lastError = "Hash error";
		}
	}

	void CSignOpenSSLRSA::subscribe()
	{
		if (this->sign)
		{
			delete[]  this->sign;
		}

		if (1 != EVP_DigestSignFinal(mdctx, NULL, &this->lenSign))
		{
			this->_isError = true;
			this->codeError = 4001;
			this->lastError = "Error get len sign";
		}

		this->sign = new unsigned char[this->lenSign + 1];
		if (!this->sign)
		{
			this->_isError = true;
			this->codeError = 4002;
			this->lastError = "Error memory";
		}

		if (1 != EVP_DigestSignFinal(mdctx, this->sign, &this->lenSign))
		{
			this->_isError = true;
			this->codeError = 4003;
			this->lastError = "Error get sign";
		}
	}

	void CSignOpenSSLRSA::SetPrivetKey(unsigned char * key, size_t keyLen)
	{
		setKeyFromBytes(key, keyLen);
	}
}
