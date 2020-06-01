#pragma once
#include "YSecurityTypes.h"
#include "YDigest.h"
namespace YSecurity
{
	class WrapperDigest
	{
		CYDigest * digest;
	public:
		WrapperDigest();
		WrapperDigest(char * name);
		yBytes getHash(yBytes & data);
		~WrapperDigest();
	};
}

