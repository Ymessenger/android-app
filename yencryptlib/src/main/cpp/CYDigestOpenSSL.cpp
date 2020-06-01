#include "CYDigestOpenSSL.h"
#include "CYException.h"


void YSecurity::CYDigestOpenSSL::setCtx(const EVP_MD * md)
{
	if (!md) {
		throw YSecurity::CYAlogitmExistedException();
	}

	mdctx = EVP_MD_CTX_new();

	if (!mdctx)
	{
		throw YSecurity::CYException("Error create context md");
	}

	if (EVP_DigestInit_ex(mdctx, md, NULL)!=1)
	{
		throw YSecurity::CYException("Error init context md");
	}
}

YSecurity::CYDigestOpenSSL::CYDigestOpenSSL() : mdctx(nullptr)
{
	const EVP_MD * md = EVP_sha512();
	setCtx(md);
}

YSecurity::CYDigestOpenSSL::CYDigestOpenSSL(char * hashName) : mdctx(nullptr)
{
	const EVP_MD * md = EVP_get_digestbyname(hashName);
	setCtx(md);
}

void YSecurity::CYDigestOpenSSL::addMsg(void * data, unsigned int lenData)
{
	if (EVP_DigestUpdate(mdctx, data, lenData) != 1)
	{
		throw YSecurity::CYException("Error update context md");
	}
}

void YSecurity::CYDigestOpenSSL::getDigest(void * byffer, unsigned int & lenBuffer)
{
	if (lenBuffer < EVP_MAX_MD_SIZE)
	{
		throw YSecurity::CYException("Buffer md is to small");
	}
	if (EVP_DigestFinal_ex(mdctx, (unsigned char *)byffer, &lenBuffer)!=1)
	{
		throw YSecurity::CYException("Error get md");
	}
}

unsigned int YSecurity::CYDigestOpenSSL::getMaxSizeBuffer()
{
	return EVP_MAX_MD_SIZE;
}

YSecurity::CYDigestOpenSSL::~CYDigestOpenSSL()
{
	if(mdctx)
		EVP_MD_CTX_free(mdctx);
}
