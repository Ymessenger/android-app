#include "WrapperDigest.h"
#include "CYDigestOpenSSL.h"


YSecurity::WrapperDigest::WrapperDigest() : digest(nullptr)
{
	digest = new CYDigestOpenSSL();
}

YSecurity::WrapperDigest::WrapperDigest(char * name) : digest(nullptr)
{
	digest = new CYDigestOpenSSL(name);
}

YSecurity::yBytes YSecurity::WrapperDigest::getHash(yBytes & data)
{
	digest->addMsg(data.getData(), data.getLenData());
	yUint len = digest->getMaxSizeBuffer();
	yBytes buffer(len);
	digest->getDigest(buffer.getData(), len);
	return yBytes(buffer.getData(), len);
}

YSecurity::WrapperDigest::~WrapperDigest()
{
	if (digest)
	{
		delete digest;
	}
}
