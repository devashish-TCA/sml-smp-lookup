#!/bin/bash

# Peppol-Commons Library Monitoring and Update Script
# Monitors for updates, security advisories, and compatibility issues

set -euo pipefail

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
PEPPOL_COMMONS_GROUP="com.helger"
GITHUB_API_BASE="https://api.github.com/repos/phax"
NOTIFICATION_EMAIL="${NOTIFICATION_EMAIL:-ops-team@yourcompany.com}"
SLACK_WEBHOOK="${SLACK_WEBHOOK:-}"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Logging function
log() {
    echo -e "${BLUE}[$(date +'%Y-%m-%d %H:%M:%S')]${NC} $1"
}

error() {
    echo -e "${RED}[ERROR]${NC} $1" >&2
}

warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

# Check current peppol-commons versions
check_current_versions() {
    log "Checking current peppol-commons versions..."
    
    cd "$PROJECT_ROOT"
    
    # Extract peppol-commons dependencies from pom.xml
    mvn dependency:tree -Dincludes="$PEPPOL_COMMONS_GROUP:*" -DoutputType=text | \
        grep "$PEPPOL_COMMONS_GROUP" | \
        sed 's/.*\(com\.helger:[^:]*:[^:]*\).*/\1/' | \
        sort -u > /tmp/current_peppol_versions.txt
    
    if [[ -s /tmp/current_peppol_versions.txt ]]; then
        success "Current peppol-commons dependencies:"
        cat /tmp/current_peppol_versions.txt
    else
        error "No peppol-commons dependencies found"
        return 1
    fi
}

# Check for available updates
check_for_updates() {
    log "Checking for peppol-commons updates..."
    
    cd "$PROJECT_ROOT"
    
    # Check for dependency updates
    mvn versions:display-dependency-updates -Dincludes="$PEPPOL_COMMONS_GROUP:*" > /tmp/peppol_updates.txt 2>&1
    
    if grep -q "No dependencies in Dependencies have newer versions" /tmp/peppol_updates.txt; then
        success "All peppol-commons dependencies are up to date"
        return 0
    elif grep -q "The following dependencies in Dependencies have newer versions" /tmp/peppol_updates.txt; then
        warning "Updates available for peppol-commons dependencies:"
        grep -A 20 "The following dependencies in Dependencies have newer versions" /tmp/peppol_updates.txt | \
            grep "$PEPPOL_COMMONS_GROUP" || true
        return 1
    else
        error "Unable to check for updates"
        cat /tmp/peppol_updates.txt
        return 2
    fi
}

# Check GitHub releases for security advisories
check_github_releases() {
    local repo="$1"
    log "Checking GitHub releases for $repo..."
    
    # Get latest releases
    curl -s "$GITHUB_API_BASE/$repo/releases?per_page=5" > "/tmp/${repo}_releases.json"
    
    if [[ -s "/tmp/${repo}_releases.json" ]]; then
        # Check for security-related releases
        if jq -r '.[].body' "/tmp/${repo}_releases.json" | grep -i -E "(security|vulnerability|cve|fix)" > "/tmp/${repo}_security.txt"; then
            warning "Security-related releases found for $repo:"
            cat "/tmp/${repo}_security.txt"
            return 1
        else
            success "No recent security advisories found for $repo"
            return 0
        fi
    else
        error "Unable to fetch releases for $repo"
        return 2
    fi
}

# Check CVE databases
check_cve_database() {
    log "Checking CVE database for peppol-commons vulnerabilities..."
    
    # Use NIST NVD API to check for vulnerabilities
    local search_terms=("peppol-commons" "helger" "peppol")
    local found_cves=false
    
    for term in "${search_terms[@]}"; do
        log "Searching CVE database for: $term"
        
        # Search recent CVEs (last 30 days)
        local start_date=$(date -d '30 days ago' '+%Y-%m-%d')
        local end_date=$(date '+%Y-%m-%d')
        
        curl -s "https://services.nvd.nist.gov/rest/json/cves/1.0?keyword=$term&pubStartDate=${start_date}T00:00:00:000%20UTC&pubEndDate=${end_date}T23:59:59:999%20UTC" > "/tmp/cve_${term}.json"
        
        if jq -r '.result.CVE_Items[].cve.CVE_data_meta.ID' "/tmp/cve_${term}.json" 2>/dev/null | grep -q "CVE"; then
            warning "CVEs found for $term:"
            jq -r '.result.CVE_Items[] | "\(.cve.CVE_data_meta.ID): \(.cve.description.description_data[0].value)"' "/tmp/cve_${term}.json" 2>/dev/null
            found_cves=true
        fi
    done
    
    if [[ "$found_cves" == "false" ]]; then
        success "No recent CVEs found for peppol-commons"
        return 0
    else
        return 1
    fi
}

# Run compatibility tests
run_compatibility_tests() {
    log "Running peppol-commons compatibility tests..."
    
    cd "$PROJECT_ROOT"
    
    # Run specific peppol-commons integration tests
    local test_classes=(
        "PeppolCommonsBasicIntegrationTest"
        "PeppolCommonsIntegrationTest"
        "PeppolCommonsPerformanceBenchmarkTest"
    )
    
    local failed_tests=()
    
    for test_class in "${test_classes[@]}"; do
        log "Running $test_class..."
        if mvn test -Dtest="$test_class" -q; then
            success "$test_class passed"
        else
            error "$test_class failed"
            failed_tests+=("$test_class")
        fi
    done
    
    if [[ ${#failed_tests[@]} -eq 0 ]]; then
        success "All peppol-commons compatibility tests passed"
        return 0
    else
        error "Failed tests: ${failed_tests[*]}"
        return 1
    fi
}

# Generate update report
generate_report() {
    local report_file="/tmp/peppol_commons_report_$(date +%Y%m%d_%H%M%S).txt"
    
    log "Generating peppol-commons monitoring report..."
    
    cat > "$report_file" << EOF
Peppol-Commons Library Monitoring Report
Generated: $(date)
Project: Peppol SML/SMP Lookup Service

=== Current Versions ===
EOF
    
    if [[ -f /tmp/current_peppol_versions.txt ]]; then
        cat /tmp/current_peppol_versions.txt >> "$report_file"
    fi
    
    cat >> "$report_file" << EOF

=== Available Updates ===
EOF
    
    if [[ -f /tmp/peppol_updates.txt ]]; then
        grep -A 20 "The following dependencies in Dependencies have newer versions" /tmp/peppol_updates.txt | \
            grep "$PEPPOL_COMMONS_GROUP" >> "$report_file" 2>/dev/null || echo "No updates available" >> "$report_file"
    fi
    
    cat >> "$report_file" << EOF

=== Security Advisories ===
EOF
    
    # Add security findings if any
    find /tmp -name "*_security.txt" -exec cat {} \; >> "$report_file" 2>/dev/null || echo "No security advisories found" >> "$report_file"
    
    cat >> "$report_file" << EOF

=== CVE Findings ===
EOF
    
    find /tmp -name "cve_*.json" -exec jq -r '.result.CVE_Items[] | "\(.cve.CVE_data_meta.ID): \(.cve.description.description_data[0].value)"' {} \; >> "$report_file" 2>/dev/null || echo "No CVEs found" >> "$report_file"
    
    cat >> "$report_file" << EOF

=== Recommendations ===
EOF
    
    # Add recommendations based on findings
    if grep -q "newer versions" /tmp/peppol_updates.txt 2>/dev/null; then
        echo "- Consider updating peppol-commons dependencies" >> "$report_file"
        echo "- Test updates in development environment first" >> "$report_file"
        echo "- Review release notes for breaking changes" >> "$report_file"
    fi
    
    if find /tmp -name "*_security.txt" -exec test -s {} \; 2>/dev/null; then
        echo "- Review security advisories and plan updates" >> "$report_file"
        echo "- Prioritize security-related updates" >> "$report_file"
    fi
    
    echo "Report generated: $report_file"
    
    # Send report via email if configured
    if [[ -n "$NOTIFICATION_EMAIL" ]]; then
        send_email_notification "$report_file"
    fi
    
    # Send to Slack if configured
    if [[ -n "$SLACK_WEBHOOK" ]]; then
        send_slack_notification "$report_file"
    fi
}

# Send email notification
send_email_notification() {
    local report_file="$1"
    
    log "Sending email notification to $NOTIFICATION_EMAIL..."
    
    if command -v mail >/dev/null 2>&1; then
        mail -s "Peppol-Commons Monitoring Report - $(date +%Y-%m-%d)" "$NOTIFICATION_EMAIL" < "$report_file"
        success "Email notification sent"
    else
        warning "Mail command not available, skipping email notification"
    fi
}

# Send Slack notification
send_slack_notification() {
    local report_file="$1"
    
    log "Sending Slack notification..."
    
    local summary=$(head -20 "$report_file" | tail -10)
    local payload=$(jq -n \
        --arg text "Peppol-Commons Monitoring Report" \
        --arg summary "$summary" \
        '{
            "text": $text,
            "attachments": [{
                "color": "warning",
                "fields": [{
                    "title": "Summary",
                    "value": $summary,
                    "short": false
                }]
            }]
        }')
    
    if curl -X POST -H 'Content-type: application/json' --data "$payload" "$SLACK_WEBHOOK" >/dev/null 2>&1; then
        success "Slack notification sent"
    else
        error "Failed to send Slack notification"
    fi
}

# Update peppol-commons dependencies (interactive)
update_dependencies() {
    log "Starting interactive peppol-commons update process..."
    
    cd "$PROJECT_ROOT"
    
    # Show available updates
    if ! check_for_updates; then
        echo
        read -p "Do you want to proceed with updates? (y/N): " -n 1 -r
        echo
        
        if [[ $REPLY =~ ^[Yy]$ ]]; then
            log "Updating peppol-commons dependencies..."
            
            # Create backup branch
            local backup_branch="backup-peppol-commons-$(date +%Y%m%d_%H%M%S)"
            git checkout -b "$backup_branch"
            git push origin "$backup_branch"
            
            # Update dependencies
            mvn versions:use-latest-versions -Dincludes="$PEPPOL_COMMONS_GROUP:*"
            
            # Run tests
            if run_compatibility_tests; then
                success "Updates completed successfully"
                
                # Commit changes
                git add pom.xml
                git commit -m "Update peppol-commons dependencies - $(date +%Y-%m-%d)"
                
                echo "Changes committed. Review and push when ready."
            else
                error "Tests failed after update. Rolling back..."
                git checkout HEAD~1 -- pom.xml
                git checkout main
                git branch -D "$backup_branch"
            fi
        else
            log "Update cancelled by user"
        fi
    fi
}

# Main function
main() {
    local action="${1:-monitor}"
    
    case "$action" in
        "monitor")
            log "Starting peppol-commons monitoring..."
            check_current_versions
            check_for_updates
            check_github_releases "peppol-commons"
            check_cve_database
            run_compatibility_tests
            generate_report
            ;;
        "update")
            update_dependencies
            ;;
        "test")
            run_compatibility_tests
            ;;
        "report")
            generate_report
            ;;
        *)
            echo "Usage: $0 [monitor|update|test|report]"
            echo "  monitor: Check for updates and security advisories (default)"
            echo "  update:  Interactively update peppol-commons dependencies"
            echo "  test:    Run compatibility tests only"
            echo "  report:  Generate monitoring report only"
            exit 1
            ;;
    esac
}

# Cleanup function
cleanup() {
    log "Cleaning up temporary files..."
    rm -f /tmp/current_peppol_versions.txt
    rm -f /tmp/peppol_updates.txt
    rm -f /tmp/*_releases.json
    rm -f /tmp/*_security.txt
    rm -f /tmp/cve_*.json
}

# Set up cleanup trap
trap cleanup EXIT

# Run main function
main "$@"