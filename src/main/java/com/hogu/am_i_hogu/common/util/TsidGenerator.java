package com.hogu.am_i_hogu.common.util;

import com.github.f4b6a3.tsid.TsidCreator;
import org.springframework.stereotype.Component;

@Component
public class TsidGenerator {

	public long nextId() {
		return TsidCreator.getTsid().toLong();
	}
}
