alter table environment_subscription drop constraint environment_subscription_subscription_id_fkey;

alter table environment_subscription add constraint environment_subscription_subscription_id_fkey
foreign key(subscription_id)
references contract_subscription(id)
on delete cascade;