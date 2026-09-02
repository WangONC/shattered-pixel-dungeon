package com.shatteredpixel.shatteredpixeldungeon.qa;

import java.util.ArrayList;
import java.util.EnumSet;

public class BuildAnalysis {
	public enum Classification { VALID, RISKY, BROKEN }

	public static class Finding {
		public Classification severity;
		public String code;
		public String reason;
		public String ruleId;

		public Finding() {}

		public Finding(Classification severity, String code, String reason, String ruleId) {
			this.severity = severity;
			this.code = code;
			this.reason = reason;
			this.ruleId = ruleId;
		}
	}

	public String buildId;
	public Classification classification = Classification.VALID;
	public final ArrayList<Finding> findings = new ArrayList<>();
	public final EnumSet<RuleQaCapability> capabilities = EnumSet.noneOf(RuleQaCapability.class);

	public void add(Classification severity, String code, String reason, String ruleId) {
		findings.add(new Finding(severity, code, reason, ruleId));
		if (severity.ordinal() > classification.ordinal()) classification = severity;
	}

	public boolean has(String code) {
		for (Finding finding : findings) if (code.equals(finding.code)) return true;
		return false;
	}
}
