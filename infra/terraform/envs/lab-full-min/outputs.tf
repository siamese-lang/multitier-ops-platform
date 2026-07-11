output "vpc_id" {
  description = "VPC ID for lab-full-min."
  value       = aws_vpc.this.id
}

output "public_subnet_id" {
  description = "Public subnet ID for bastion-01, nginx-01, and NAT Gateway."
  value       = aws_subnet.public.id
}

output "private_app_subnet_id" {
  description = "Private app subnet ID for app-01 and app-02."
  value       = aws_subnet.private_app.id
}

output "private_db_subnet_id" {
  description = "Private DB subnet ID for db-primary-01."
  value       = aws_subnet.private_db.id
}

output "security_group_ids" {
  description = "Security group IDs by tier."
  value = {
    bastion = aws_security_group.bastion.id
    nginx   = aws_security_group.nginx.id
    app     = aws_security_group.app.id
    db      = aws_security_group.db.id
  }
}

output "nat_gateway_id" {
  description = "NAT Gateway ID used for private app and DB subnet egress."
  value       = aws_nat_gateway.this.id
}

output "nat_eip_public_ip" {
  description = "Elastic IP address associated with the NAT Gateway."
  value       = aws_eip.nat.public_ip
}

output "ubuntu_ami_id" {
  description = "Ubuntu AMI selected by the data source."
  value       = data.aws_ami.ubuntu.id
}

output "bastion_public_ip" {
  description = "Public IPv4 address for bastion-01."
  value       = aws_instance.bastion.public_ip
}

output "bastion_private_ip" {
  description = "Private IPv4 address for bastion-01."
  value       = aws_instance.bastion.private_ip
}

output "nginx_public_ip" {
  description = "Public IPv4 address for nginx-01."
  value       = aws_instance.nginx.public_ip
}

output "nginx_private_ip" {
  description = "Private IPv4 address for nginx-01."
  value       = aws_instance.nginx.private_ip
}

output "app_private_ips" {
  description = "Private IPv4 addresses for app nodes."
  value       = { for name, instance in aws_instance.app : name => instance.private_ip }
}

output "db_primary_private_ip" {
  description = "Private IPv4 address for db-primary-01."
  value       = aws_instance.db_primary.private_ip
}

output "tier_flow_summary" {
  description = "Expected network flow for lab-full-min."
  value = {
    operator_to_bastion = "${var.operator_cidr} -> bastion-01:22"
    web_to_nginx_http   = "${var.web_ingress_cidr} -> nginx-01:${var.web_port}"
    web_to_nginx_https  = "${var.web_ingress_cidr} -> nginx-01:${var.web_https_port}"
    bastion_to_nodes    = "bastion SG -> nginx/app/db:22"
    nginx_to_app        = "nginx SG -> app SG:${var.app_port}"
    app_to_db           = "app SG -> db SG:${var.db_port}"
    private_egress      = "app/db private subnets -> NAT Gateway"
  }
}

output "ssh_to_bastion" {
  description = "SSH command template for bastion-01. Replace <path-to-private-key> before use."
  value       = "ssh -i <path-to-private-key> ubuntu@${aws_instance.bastion.public_ip}"
}

output "ssh_to_nginx_via_bastion_proxycommand" {
  description = "ProxyCommand SSH template for nginx-01 through bastion-01. Replace <path-to-private-key> before use."
  value       = "ssh -i <path-to-private-key> -o IdentitiesOnly=yes -o ProxyCommand=\"ssh -i <path-to-private-key> -o IdentitiesOnly=yes -W %h:%p ubuntu@${aws_instance.bastion.public_ip}\" ubuntu@${aws_instance.nginx.private_ip}"
}

output "ssh_to_app_via_bastion_proxycommand" {
  description = "ProxyCommand SSH templates for app nodes through bastion-01. Replace <path-to-private-key> before use."
  value = {
    for name, instance in aws_instance.app : name => "ssh -i <path-to-private-key> -o IdentitiesOnly=yes -o ProxyCommand=\"ssh -i <path-to-private-key> -o IdentitiesOnly=yes -W %h:%p ubuntu@${aws_instance.bastion.public_ip}\" ubuntu@${instance.private_ip}"
  }
}

output "ssh_to_db_via_bastion_proxycommand" {
  description = "ProxyCommand SSH template for db-primary-01 through bastion-01. Replace <path-to-private-key> before use."
  value       = "ssh -i <path-to-private-key> -o IdentitiesOnly=yes -o ProxyCommand=\"ssh -i <path-to-private-key> -o IdentitiesOnly=yes -W %h:%p ubuntu@${aws_instance.bastion.public_ip}\" ubuntu@${aws_instance.db_primary.private_ip}"
}
