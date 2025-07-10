#!/bin/bash

# Debug Script for The Curse Plugin
# Interactive testing and debugging commands

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

print_status() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Check if server is running
if ! screen -list | grep -q "minecraft"; then
    print_error "Minecraft server is not running. Start it with 'make start'"
    exit 1
fi

print_success "The Curse Plugin Debug Console"
print_status "Server is running. Use these commands to test the plugin:"
echo ""

# Function to send command to server
send_command() {
    local cmd="$1"
    print_status "Sending command: $cmd"
    screen -S minecraft -p 0 -X stuff "$cmd$(printf \\r)"
    sleep 1
}

# Function to show logs
show_recent_logs() {
    if [[ -f "server/logs/latest.log" ]]; then
        echo ""
        print_status "Recent server logs:"
        tail -n 10 "server/logs/latest.log"
        echo ""
    fi
}

# Interactive menu
while true; do
    echo ""
    echo "=== The Curse Plugin Debug Menu ==="
    echo "1. Test curse start command"
    echo "2. Test curse stop command"
    echo "3. Test leaderboard command"
    echo "4. Give Bad Omen potion to player"
    echo "5. Set time to night"
    echo "6. Show plugin status"
    echo "7. Show recent logs"
    echo "8. Reload plugin"
    echo "9. Op a player"
    echo "10. List online players"
    echo "11. Custom command"
    echo "0. Exit"
    echo ""
    read -p "Choose an option: " choice

    case $choice in
        1)
            read -p "Enter player name: " player
            send_command "curse start $player"
            show_recent_logs
            ;;
        2)
            read -p "Enter player name: " player
            send_command "curse stop $player"
            show_recent_logs
            ;;
        3)
            send_command "curse leaderboard"
            show_recent_logs
            ;;
        4)
            read -p "Enter player name: " player
            send_command "give $player minecraft:potion{Potion:\"minecraft:bad_omen\"} 1"
            print_success "Gave Bad Omen potion to $player"
            show_recent_logs
            ;;
        5)
            send_command "time set night"
            print_success "Set time to night"
            show_recent_logs
            ;;
        6)
            send_command "plugins"
            show_recent_logs
            ;;
        7)
            show_recent_logs
            ;;
        8)
            send_command "curse reload"
            show_recent_logs
            ;;
        9)
            read -p "Enter player name to op: " player
            send_command "op $player"
            print_success "Gave op to $player"
            show_recent_logs
            ;;
        10)
            send_command "list"
            show_recent_logs
            ;;
        11)
            read -p "Enter custom command: " cmd
            send_command "$cmd"
            show_recent_logs
            ;;
        0)
            print_status "Exiting debug console"
            break
            ;;
        *)
            print_error "Invalid option"
            ;;
    esac
done

print_status "Debug session ended"
