#pragma once
#include "YSecurityElementaryTypes.h"

namespace YSecurity
{
	class YTime
	{
	public:
		static YSecurity::date getDateTime();
		static bool isAlive(YSecurity::date timeCreate, YSecurity::uint_32 timeLife);

		static bool isCorrectKey(YSecurity::date timeCreateMsg, YSecurity::date timeCreateKey, YSecurity::uint_32 timeLifeKey, YSecurity::uint_32 limit=60);
	};
}