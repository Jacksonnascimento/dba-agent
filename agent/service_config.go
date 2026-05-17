package main

import (
	"strings"
	"unicode"
)

const (
	defaultServiceName        = "DBAAgentWorker"
	defaultDisplayName        = "DBA Agent Worker"
	maxInternalServiceNameLen = 80
	maxDisplayNameLen         = 256
)

// serviceNamesFromAgent derives OS service identifiers from the agent name chosen in the dashboard.
// internalName is SCM-safe; displayName is shown in the services console.
func serviceNamesFromAgent(agentName string) (internalName, displayName string) {
	trimmed := strings.TrimSpace(agentName)
	if trimmed == "" {
		return defaultServiceName, defaultDisplayName
	}

	displayName = trimmed
	if len(displayName) > maxDisplayNameLen {
		displayName = displayName[:maxDisplayNameLen]
	}

	var b strings.Builder
	b.WriteString("DBAAgent_")
	for _, r := range trimmed {
		switch {
		case unicode.IsLetter(r) || unicode.IsDigit(r):
			b.WriteRune(r)
		case r == '-' || r == '_' || unicode.IsSpace(r):
			b.WriteRune('_')
		}
	}
	internalName = strings.Trim(b.String(), "_")
	if internalName == "DBAAgent" || internalName == "" {
		return defaultServiceName, displayName
	}
	if len(internalName) > maxInternalServiceNameLen {
		internalName = internalName[:maxInternalServiceNameLen]
	}
	return internalName, displayName
}
