package main

import "testing"

func TestServiceNamesFromAgent(t *testing.T) {
	internal, display := serviceNamesFromAgent("SRV-PROD-SQL-01")
	if internal != "DBAAgent_SRV_PROD_SQL_01" {
		t.Fatalf("internal = %q, want DBAAgent_SRV_PROD_SQL_01", internal)
	}
	if display != "SRV-PROD-SQL-01" {
		t.Fatalf("display = %q", display)
	}

	internal2, display2 := serviceNamesFromAgent("")
	if internal2 != defaultServiceName || display2 != defaultDisplayName {
		t.Fatalf("empty name fallback failed: %q / %q", internal2, display2)
	}

	internal3, _ := serviceNamesFromAgent("!!!")
	if internal3 != defaultServiceName {
		t.Fatalf("symbols-only fallback = %q", internal3)
	}
}
