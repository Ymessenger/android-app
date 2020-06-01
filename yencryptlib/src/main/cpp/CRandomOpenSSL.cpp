#include "CRandomOpenSSL.h"
#include <openssl/rand.h>
#include "CYException.h"

void YSecurity::CRandomOpenSSL::seed(YSecurity::ubyte * seed, YSecurity::uint_32 lenSeed)
{
	RAND_seed(seed, lenSeed);
}

void YSecurity::CRandomOpenSSL::addRandom(YSecurity::ubyte * seed, YSecurity::uint_32 lenSeed)
{
	RAND_add(seed, lenSeed, lenSeed/10);
}

bool YSecurity::CRandomOpenSSL::randomStatus()
{
	return RAND_status()==1;
}

void YSecurity::CRandomOpenSSL::getBytes(YSecurity::ubyte * data, YSecurity::uint_32 len)
{
	if (!randomStatus())
	{
		throw YSecurity::CYRandomStatusException();
	}
	else
	{
		if (RAND_priv_bytes(data, len)!=1)
		{
			throw YSecurity::CYGenerateKeyException("Could not generate symmetric key");
		}
	}
}
