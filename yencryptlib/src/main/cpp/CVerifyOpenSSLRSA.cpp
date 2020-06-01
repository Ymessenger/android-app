#include "CVerifyOpenSSLRSA.h"
#include <openssl/rsa.h>
#include <openssl/x509.h>
#include <cmath>

namespace YSecurity
{
	void CVerifyOpenSSLRSA::setKeyFromBytes(unsigned char * key, size_t keyLen)
	{
		this->isVerify = false;
		this->correct = false;

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

		rsa_st * rsa;
		const unsigned char * p = key;
		this->pkey = d2i_PUBKEY(0, &p, keyLen);
		if (!this->pkey)
		{
			this->_isError = true;
			this->codeError = 1202;
			this->lastError = "Error pkey create";
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
		if (1 != EVP_DigestVerifyInit(mdctx, NULL, md, NULL, pkey))
		{
			this->_isError = true;
			this->codeError = 1301;
			this->lastError = "Error Init context Verify";
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
		
		int lenSalt = ceil((RSA_size(rsa) * 8 - 1) / 8.0) - EVP_MD_size(md) - 2;

		if (EVP_PKEY_CTX_set_rsa_pss_saltlen(ctx, lenSalt) <= 0)
		{
			this->_isError = true;
			this->codeError = 1403;
			this->lastError = "Error set len salt";
			return;
		}
	}

	CVerifyOpenSSLRSA::CVerifyOpenSSLRSA(unsigned char * sign, size_t sLen, unsigned char * key, size_t kLen, const char * mdName)
	{
		this->mdName = mdName;
		mdctx = NULL;
		pkey = NULL;
		this->sign = NULL;
		this->lenSign = 0;

		this->lastError = "";
		this->_isError = false;
		this->codeError = 0;

		this->setSign(sign, sLen);
		if (!this->_isError)
		{
			setKeyFromBytes(key, kLen);
		}
	}

	CVerifyOpenSSLRSA::~CVerifyOpenSSLRSA()
	{
		if (mdctx)
			EVP_MD_CTX_free(mdctx);
		if (pkey)
			EVP_PKEY_free(pkey);
	}

	void CVerifyOpenSSLRSA::setPublicKey(unsigned char * sign, size_t sLen)
	{
		setKeyFromBytes(sign, sLen);
	}

	void CVerifyOpenSSLRSA::addMsg(unsigned char * msg, size_t lenMsg)
	{
		if (this->_isError)
		{
			return;
		}
		this->isVerify = false;
		if (1 != EVP_DigestVerifyUpdate(mdctx, (char*)msg, lenMsg))
		{
			this->_isError = true;
			this->codeError = 2001;
			this->lastError = "Hash error";
		}
	}

	void CVerifyOpenSSLRSA::verify()
	{
		if (this->_isError)
		{
			return;
		}

		if (1 == EVP_DigestVerifyFinal(mdctx, this->sign, this->lenSign))
		{
			this->correct = true;
		}
		else
		{
			this->correct = false;
		}
		this->isVerify = true;
	}
}
