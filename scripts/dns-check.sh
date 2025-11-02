#!/usr/bin/env bash

# Usage: ./dns_check.sh netward.eu ns1.netward.eu ns2.netward.eu
domain=$1
shift
nameservers=("$@")

if [ -z "$domain" ] || [ ${#nameservers[@]} -eq 0 ]; then
    echo "Usage: $0 <domain> <ns1> [ns2 ...]"
    exit 1
fi

while true; do
    clear
    echo "🕒 $(date)"
    echo "---------------------------------------------"

    echo "🔎 Checking delegation for $domain..."
    ns_records=$(dig +short NS "$domain")
    if [ -z "$ns_records" ]; then
        echo "❌ No NS records yet for $domain"
    else
        echo "✅ NS records found:"
        echo "$ns_records" | sed 's/^/   - /'
    fi

    echo "---------------------------------------------"
    echo "🔍 Checking glue / A records for nameservers:"
    all_ok=true
    for ns in "${nameservers[@]}"; do
        ip=$(dig +short A "$ns")
        if [ -z "$ip" ]; then
            echo "❌ $ns → no A record found"
            all_ok=false
        else
            echo "✅ $ns → $ip"
        fi
    done

    echo "---------------------------------------------"
    echo "📜 Checking SOA record..."
    soa=$(dig +short SOA "$domain")
    if [ -z "$soa" ]; then
        echo "❌ No SOA record found"
        all_ok=false
    else
        echo "✅ SOA: $soa"
    fi

    echo "---------------------------------------------"
    echo "🧩 Testing authoritative responses:"
    for ns in "${nameservers[@]}"; do
        auth_check=$(dig @"$ns" "$domain" SOA +norecurse +short 2>/dev/null)
        if [ -z "$auth_check" ]; then
            echo "❌ $ns does not answer authoritatively"
            all_ok=false
        else
            echo "✅ $ns answers authoritatively for $domain"
        fi
    done

    echo "---------------------------------------------"
    echo "📦 Testing full resolution (A record for domain):"
    a_record=$(dig +short A "$domain")
    if [ -z "$a_record" ]; then
        echo "❌ No A record found for $domain"
    else
        echo "✅ $domain → $a_record"
    fi

    echo "---------------------------------------------"
    if [ "$all_ok" = true ]; then
        echo "🎉 All checks passed — DNS looks healthy!"
        break
    else
        echo "⌛ Waiting 30 seconds before next check..."
        sleep 30
    fi
done
