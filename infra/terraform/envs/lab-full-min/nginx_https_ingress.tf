variable "web_https_port" {
  description = "Public HTTPS port exposed by nginx-01 for lab validation."
  type        = number
  default     = 443
}

resource "aws_security_group_rule" "nginx_https_from_web_ingress_cidr" {
  type              = "ingress"
  description       = "HTTPS from allowed web ingress CIDR"
  security_group_id = aws_security_group.nginx.id
  from_port         = var.web_https_port
  to_port           = var.web_https_port
  protocol          = "tcp"
  cidr_blocks       = [var.web_ingress_cidr]
}
