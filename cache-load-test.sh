#!/bin/bash

# Cache Performance Load Testing Script
# This script generates load on your cached endpoints to analyze cache benefits

SERVER_URL="${1:-http://localhost:8080/api/v1}"
echo "🚀 Starting Cache Performance Analysis Load Test"
echo "🎯 Target Server: $SERVER_URL"
echo "📊 This will help you analyze pre-cache vs post-cache performance"
echo ""

# Function to login and get JWT token
get_jwt_token() {
    echo "🔐 Logging in to get JWT token..."

    # Replace with your actual login credentials
    LOGIN_RESPONSE=$(curl -s -X POST "$SERVER_URL/auth/login" \
        -H "Content-Type: application/json" \
        -d '{
            "email": "someone@gmail.com",
            "password": "P@$$w0rd"
        }')

    # Extract token from response without jq (using grep and sed)
    JWT_TOKEN=$(echo "$LOGIN_RESPONSE" | grep -o '"token":"[^"]*"' | sed 's/"token":"//;s/"//')

    if [ -z "$JWT_TOKEN" ]; then
        echo "❌ Failed to get JWT token. Please check login credentials."
        echo "Response: $LOGIN_RESPONSE"
        exit 1
    else
        echo "✅ Successfully obtained JWT token"
        export JWT_TOKEN
    fi
}

# Call login function if no token is set
if [ -z "$JWT_TOKEN" ]; then
    get_jwt_token
fi

# Function to make API calls
make_requests() {
    local endpoint=$1
    local count=$2
    local description=$3

    echo "📡 Making $count requests to $description..."
    echo "   Full URL: $SERVER_URL$endpoint"

    for i in $(seq 1 $count); do
        # Make the actual request and capture response code
        if [ ! -z "$JWT_TOKEN" ]; then
            response_code=$(curl -s -o /dev/null -w "%{http_code}" \
                -H "Authorization: Bearer $JWT_TOKEN" \
                "$SERVER_URL$endpoint")
        else
            response_code=$(curl -s -o /dev/null -w "%{http_code}" "$SERVER_URL$endpoint")
        fi

        if [ "$response_code" != "200" ]; then
            echo "   ⚠️  Request $i failed with HTTP $response_code"
        fi

        if [ $((i % 10)) -eq 0 ]; then
            echo "   Progress: $i/$count requests completed"
        fi
    done
    echo "✅ Completed $count requests to $description"
    echo ""
}

# Reset cache statistics first
echo "🔄 Resetting cache statistics..."
curl -s -X DELETE "$SERVER_URL/admin/cache-analysis/reset" > /dev/null

# Capture baseline (pre-cache benefits)
echo "📸 Capturing baseline performance..."
curl -s -X POST "$SERVER_URL/admin/cache-analysis/baseline" > /dev/null

# Generate load on cached endpoints
echo "🏃‍♂️ Generating load to demonstrate cache benefits..."
echo ""

# Products cache testing
make_requests "/admin/products?limit=10&offset=0" 25 "Products (paginated)"
make_requests "/admin/products?limit=20&offset=0" 15 "Products (different pagination)"
make_requests "/admin/products?limit=10&offset=10" 20 "Products (page 2)"

# Categories cache testing
make_requests "/categories?limit=5&offset=0" 30 "Categories"
make_requests "/categories?limit=10&offset=0" 20 "Categories (different limit)"

# Orders cache testing (if you have authentication set up)
make_requests "/admin/orders?limit=15&offset=0" 20 "Orders (admin)"
make_requests "/admin/orders?limit=10&offset=5" 15 "Orders (different pagination)"

# Repeat some requests to show cache hits
echo "🔁 Repeating requests to generate cache hits..."
make_requests "/admin/products?limit=10&offset=0" 30 "Products (repeat for cache hits)"
make_requests "/categories?limit=5&offset=0" 25 "Categories (repeat for cache hits)"

echo "✅ Load generation completed!"
echo ""
echo "📊 Now you can analyze the cache performance:"
echo ""
echo "Current Stats:"
echo "curl $SERVER_URL/admin/cache-analysis/current-stats | jq"
echo ""
echo "Performance Comparison (Pre vs Post Cache):"
echo "curl $SERVER_URL/admin/cache-analysis/comparison | jq"
echo ""
echo "Quick Summary:"
echo "curl $SERVER_URL/admin/cache-analysis/summary"
echo ""
echo "Recommendations:"
echo "curl $SERVER_URL/admin/cache-analysis/recommendations | jq"
echo ""
echo "🎉 Cache analysis complete! Check the endpoints above to see your cache benefits."
