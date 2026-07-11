output "vpc_id" {
  description = "VPC ID for lab-full-ops."
  value       = aws_vpc.this.id
}

output "subnet_ids" {
  description = "Subnet IDs by tier."
  value = {
    public          = aws_subnet.public.id
    private_app     = aws_subnet.private_app.id
    private_db      = aws_subnet.private_db.id
    private_storage = aws_subnet.private_storage.id
    private_ops     = aws_subnet.private_ops.id
  }
}

output "security_group_ids" {
  description = "Security group IDs by tier."
  value = {
    bastion    = aws_security_group.bastion.id
    nginx      = aws_security_group.nginx.id
    app        = aws_security_group.app.id
    db         = aws_security_group.db.id
    storage    = aws_security_group.storage.id
    backup     = aws_security_group.backup.id
    monitoring = aws_security_group.monitoring.id
    logging    = aws_security_group.logging.id
    loadgen    = aws_security_group.loadgen.id
  }
}

output "nat_gateway_id" {
  description = "NAT Gateway ID used for private subnet egress."
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

output "public_node_ips" {
  description = "Public and private IPs for public tier nodes."
  value = {
    "bastion-01" = {
      public_ip  = aws_instance.bastion.public_ip
      private_ip = aws_instance.bastion.private_ip
    }
    "nginx-01" = {
      public_ip  = aws_instance.nginx.public_ip
      private_ip = aws_instance.nginx.private_ip
    }
  }
}

output "private_node_ips" {
  description = "Private IPs for lab-full-ops private nodes."
  value = {
    app     = { for name, instance in aws_instance.app : name => instance.private_ip }
    db      = { "db-primary-01" = aws_instance.db_primary.private_ip }
    storage = { "nfs-01" = aws_instance.nfs.private_ip }
    backup  = { "backup-01" = aws_instance.backup.private_ip }
    metrics = { "mon-01" = aws_instance.monitoring.private_ip }
    logs    = { "log-01" = aws_instance.logging.private_ip }
    loadgen = { "loadgen-01" = aws_instance.loadgen.private_ip }
  }
}

output "tier_flow_summary" {
  description = "Expected network flow for lab-full-ops."
  value = {
    operator_to_bastion = "${var.operator_cidr} -> bastion-01:22"
    web_to_nginx_http   = "${var.web_ingress_cidr} -> nginx-01:${var.web_port}"
    web_to_nginx_https  = "${var.web_ingress_cidr} -> nginx-01:${var.web_https_port}"
    bastion_to_nodes    = "bastion SG -> all lab nodes:22"
    nginx_to_app        = "nginx SG -> app SG:${var.app_port}"
    app_to_db           = "app SG -> db SG:${var.db_port}"
    app_to_storage      = "app SG -> storage SG:${var.nfs_port}"
    backup_to_db        = "backup SG -> db SG:${var.db_port}"
    backup_to_storage   = "backup SG -> storage SG:${var.nfs_port}"
    loadgen_to_nginx    = "loadgen SG -> nginx SG:${var.web_https_port}"
    monitoring_scrape   = "monitoring SG -> lab node SGs:${var.node_exporter_port}"
    log_shipping        = "VPC CIDR -> log SG:${var.loki_push_port}"
    private_egress      = "private app/db/storage/ops subnets -> NAT Gateway"
  }
}

output "ssh_to_bastion" {
  description = "SSH command template for bastion-01. Replace <path-to-private-key> before use."
  value       = "ssh -i <path-to-private-key> ubuntu@${aws_instance.bastion.public_ip}"
}

output "ssh_proxycommand_templates" {
  description = "ProxyCommand SSH templates through bastion-01. Replace <path-to-private-key> before use."
  value = merge(
    {
      "nginx-01"      = "ssh -i <path-to-private-key> -o IdentitiesOnly=yes -o ProxyCommand=\"ssh -i <path-to-private-key> -o IdentitiesOnly=yes -W %h:%p ubuntu@${aws_instance.bastion.public_ip}\" ubuntu@${aws_instance.nginx.private_ip}"
      "db-primary-01" = "ssh -i <path-to-private-key> -o IdentitiesOnly=yes -o ProxyCommand=\"ssh -i <path-to-private-key> -o IdentitiesOnly=yes -W %h:%p ubuntu@${aws_instance.bastion.public_ip}\" ubuntu@${aws_instance.db_primary.private_ip}"
      "nfs-01"        = "ssh -i <path-to-private-key> -o IdentitiesOnly=yes -o ProxyCommand=\"ssh -i <path-to-private-key> -o IdentitiesOnly=yes -W %h:%p ubuntu@${aws_instance.bastion.public_ip}\" ubuntu@${aws_instance.nfs.private_ip}"
      "backup-01"     = "ssh -i <path-to-private-key> -o IdentitiesOnly=yes -o ProxyCommand=\"ssh -i <path-to-private-key> -o IdentitiesOnly=yes -W %h:%p ubuntu@${aws_instance.bastion.public_ip}\" ubuntu@${aws_instance.backup.private_ip}"
      "mon-01"        = "ssh -i <path-to-private-key> -o IdentitiesOnly=yes -o ProxyCommand=\"ssh -i <path-to-private-key> -o IdentitiesOnly=yes -W %h:%p ubuntu@${aws_instance.bastion.public_ip}\" ubuntu@${aws_instance.monitoring.private_ip}"
      "log-01"        = "ssh -i <path-to-private-key> -o IdentitiesOnly=yes -o ProxyCommand=\"ssh -i <path-to-private-key> -o IdentitiesOnly=yes -W %h:%p ubuntu@${aws_instance.bastion.public_ip}\" ubuntu@${aws_instance.logging.private_ip}"
      "loadgen-01"    = "ssh -i <path-to-private-key> -o IdentitiesOnly=yes -o ProxyCommand=\"ssh -i <path-to-private-key> -o IdentitiesOnly=yes -W %h:%p ubuntu@${aws_instance.bastion.public_ip}\" ubuntu@${aws_instance.loadgen.private_ip}"
    },
    {
      for name, instance in aws_instance.app : name => "ssh -i <path-to-private-key> -o IdentitiesOnly=yes -o ProxyCommand=\"ssh -i <path-to-private-key> -o IdentitiesOnly=yes -W %h:%p ubuntu@${aws_instance.bastion.public_ip}\" ubuntu@${instance.private_ip}"
    }
  )
}
