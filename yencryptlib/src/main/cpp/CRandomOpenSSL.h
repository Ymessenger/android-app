#pragma once
#include "YSecurityElementaryTypes.h"

namespace YSecurity
{
	class CRandomOpenSSL
	{
	public:
		static void seed(YSecurity::ubyte * seed, YSecurity::uint_32 lenSeed);
		static void addRandom(YSecurity::ubyte * seed, YSecurity::uint_32 lenSeed);
		static bool randomStatus();
		static void getBytes(YSecurity::ubyte * data, YSecurity::uint_32 len);
	};
}