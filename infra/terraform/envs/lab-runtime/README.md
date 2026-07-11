# Terraform lab-runtime

`lab-runtime` is the NAT-enabled runtime variant used before deploying OpenKoda to a private app node.

It intentionally differs from `lab-small`:

- `lab-small` proves no-NAT private app-node isolation and bastion/Ansible control.
- `lab-runtime` keeps the app node private, but adds NAT egress so runtime dependencies can be retrieved.

## Scope

This environment creates:

- VPC
- public subnet
- private subnet
- Internet Gateway
- public route table
- private route table
- Elastic IP for NAT Gateway
- NAT Gateway in the public subnet
- private default route through the NAT Gateway
- `sg-bastion`
- `sg-app`
- `bastion-01`
- `app-01`

## Security model

- `bastion-01` is in the public subnet and has a public IPv4 address.
- SSH to `bastion-01` is allowed only from `operator_cidr`.
- `app-01` is in the private subnet and has no public IPv4 address.
- SSH to `app-01` is allowed only from the bastion security group.
- TCP `8080` to `app-01` is allowed only from the bastion security group.
- `app-01` outbound internet access is available only through the private subnet route to the NAT Gateway.

## Cost warning

This environment creates a NAT Gateway and an Elastic IP.

NAT Gateway can incur hourly and data processing charges even while the lab is idle. Use this environment only for short runtime validation windows and destroy it immediately after evidence collection.

## Usage

Copy the example variable file and fill in local values:

```bash
cp terraform.tfvars.example terraform.tfvars
```

Edit:

```hcl
key_name      = "your-existing-ec2-keypair-name"
operator_cidr = "your-public-ip/32"
```

`key_name` is the EC2 key pair name in AWS, not the `.pem` file path.

## Validate and apply

```bash
terraform fmt -check
terraform init
terraform validate
terraform plan -out tfplan
terraform apply tfplan
terraform output
```

Expected plan difference from `lab-small`:

- `aws_eip.nat`
- `aws_nat_gateway.this`
- private route table default route through `aws_nat_gateway.this`

## Access checks

Use Terraform outputs:

```bash
terraform output -raw bastion_public_ip
terraform output -raw app_private_ip
terraform output ssh_to_bastion
terraform output ssh_to_app_via_bastion_proxycommand
```

From the operator workstation:

```bash
ssh -i <path-to-private-key> ubuntu@<bastion_public_ip>
```

From the operator workstation to private app through bastion:

```bash
ssh -i <path-to-private-key> \
  -o IdentitiesOnly=yes \
  -o ProxyCommand="ssh -i <path-to-private-key> -o IdentitiesOnly=yes -W %h:%p ubuntu@<bastion_public_ip>" \
  ubuntu@<app_private_ip>
```

## Runtime egress check

From `app-01`, verify that the runtime variant has outbound HTTPS egress:

```bash
hostname
hostname -I
ip route
curl -I https://google.com --max-time 5
```

Expected result:

- `hostname -I` shows only a private IP for `app-01`.
- `ip route` includes a default route through the private subnet router.
- `curl -I https://google.com --max-time 5` succeeds through NAT.

This is intentionally different from `lab-small`, where the same HTTPS egress check timed out because no NAT route existed.

## Destroy

Destroy immediately after validation:

```bash
terraform destroy
terraform state list
```

`terraform state list` should return no managed resources after destroy.

## Do not commit

Do not commit:

- `terraform.tfvars`
- `*.tfstate`
- saved plan files
- private keys
- AWS credentials
- session tokens
