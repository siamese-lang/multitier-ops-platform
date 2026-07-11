output "vpc_id" {
  description = "VPC ID for lab-runtime."
  value       = aws_vpc.this.id
}

output "public_subnet_id" {
  description = "Public subnet ID for bastion-01 and NAT Gateway."
  value       = aws_subnet.public.id
}

output "private_subnet_id" {
  description = "Private subnet ID for app-01."
  value       = aws_subnet.private.id
}

output "bastion_security_group_id" {
  description = "Security group ID attached to bastion-01."
  value       = aws_security_group.bastion.id
}

output "app_security_group_id" {
  description = "Security group ID attached to app-01."
  value       = aws_security_group.app.id
}

output "nat_gateway_id" {
  description = "NAT Gateway ID used for private app node egress."
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

output "app_private_ip" {
  description = "Private IPv4 address for app-01."
  value       = aws_instance.app.private_ip
}

output "app_private_egress_mode" {
  description = "Expected private app node egress mode for lab-runtime."
  value       = "nat-gateway"
}

output "ssh_to_bastion" {
  description = "SSH command template for bastion-01. Replace <path-to-private-key> before use."
  value       = "ssh -i <path-to-private-key> ubuntu@${aws_instance.bastion.public_ip}"
}

output "ssh_to_app_via_bastion" {
  description = "SSH jump command template for app-01 through bastion-01. Replace <path-to-private-key> before use."
  value       = "ssh -i <path-to-private-key> -J ubuntu@${aws_instance.bastion.public_ip} ubuntu@${aws_instance.app.private_ip}"
}

output "ssh_to_app_via_bastion_proxycommand" {
  description = "ProxyCommand SSH template for app-01 through bastion-01. Replace <path-to-private-key> before use."
  value       = "ssh -i <path-to-private-key> -o IdentitiesOnly=yes -o ProxyCommand=\"ssh -i <path-to-private-key> -o IdentitiesOnly=yes -W %h:%p ubuntu@${aws_instance.bastion.public_ip}\" ubuntu@${aws_instance.app.private_ip}"
}
